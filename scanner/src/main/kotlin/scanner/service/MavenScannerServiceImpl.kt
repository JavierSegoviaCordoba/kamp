package scanner.service

import kamp.domain.MavenArtifactImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import scanner.client.MavenRepositoryClient
import scanner.processor.GradleModuleProcessor
import scanner.processor.PomProcessor
import scanner.util.supervisedLaunch

class MavenScannerServiceImpl(
  override val client: MavenRepositoryClient<MavenArtifactImpl>,
  override val pomProcessor: PomProcessor,
  override val gradleModuleProcessor: GradleModuleProcessor,
) : MavenScannerService<MavenArtifactImpl>() {
  override fun CoroutineScope.produceArtifacts(rootArtefactsFilter: Set<String>?, rootArtefactsExcludeFilter: Set<String>?): ReceiveChannel<MavenArtifactImpl> = produce {
    val pageChannel = Channel<List<MavenRepositoryClient.RepoItem>>(Channel.BUFFERED)
    supervisedLaunch {
      client.listRepositoryPath("")?.filter { repoItem ->
        val path = repoItem.path.removePrefix("/")
        val included = rootArtefactsFilter
          ?.let { filter -> filter.any { path.startsWith(it) } }
          ?: true
        val excluded = rootArtefactsExcludeFilter
          ?.let { filter -> filter.any { path.startsWith(it) } }
          ?: false
        included && !excluded
      }?.let { pageChannel.send(it) }
    }
    
    // Tracker
    supervisedLaunch {
      var ticks = 0
      do {
        delay(5000)
        if (pageChannel.isEmpty) {
          logger.info("Page channel empty, ${5 - ticks} ticks remaining until close")
          ticks++
        } else {
          ticks = 0
        }
      } while (ticks < 5)
      logger.info("Closing page channel")
      pageChannel.close()
      logger.info("Closed page channel")
    }
    
    // Workers
    repeat(Runtime.getRuntime().availableProcessors() * 2) {
      supervisedLaunch {
        for (page in pageChannel) {
          val artifactDetails = page.find { it.value == "maven-metadata.xml" }?.let {
            client.getArtifactDetails(it.path)
          }
          if (artifactDetails != null) {
            logger.debug("Found MC artefact ${artifactDetails.group}:${artifactDetails.name}")
            send(artifactDetails)
          } else {
            page
              .filter(MavenRepositoryClient.RepoItem::isDirectory)
              .map {
                supervisedLaunch {
                  client.listRepositoryPath(it.path)?.let { item ->
                    logger.debug("Scanned MC page ${it.path} and found ${item.size} children")
                    pageChannel.send(item)
                  }
                }
              }
          }
        }
      }
    }
  }
}
