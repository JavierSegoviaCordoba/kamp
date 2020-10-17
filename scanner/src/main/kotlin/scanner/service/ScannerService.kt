package scanner.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.*
import scanner.client.*
import scanner.domain.*
import scanner.domain.kotlin.*
import scanner.processor.*
import scanner.util.*
import java.util.concurrent.*

abstract class ScannerService<A : MavenArtifact> {
  protected val logger by lazy { Logger(this::class) }
  protected abstract val client: MavenRepositoryClient<A>
  protected abstract fun CoroutineScope.produceArtifactChannel(): ReceiveChannel<A>
  
  suspend fun scan(onFound: suspend (KotlinMPPLibrary) -> Unit): Unit = coroutineScope {
    val artifacts = produceArtifactChannel()
    val mppLibs = produce {
      parallel {
        for (artifact in artifacts) {
          errorSafe {
            client.getGradleModule(artifact)?.let { module ->
              val targets = GradleModuleProcessor.listSupportedTargets(module)
              if (GradleModuleProcessor.isRootModule(module) && targets.isNotEmpty()) {
                val pom = client.getMavenPom(artifact)
                val pomDetails = pom?.let {
                  listOf(
                    PomProcessor.getDescription(pom),
                    PomProcessor.getUrl(pom),
                    PomProcessor.getScmUrl(pom)
                  )
                }
                pomDetails?.let {
                  val (description, website, scm) = pomDetails
                  
                  val lib = KotlinMPPLibrary(
                    group = artifact.group,
                    name = artifact.name,
                    latestVersion = artifact.latestVersion,
                    targets = targets,
                    description = description,
                    website = website,
                    scm = scm
                  )
                  logger.info { prettyJson.encodeToString(lib) }
                  send(lib)
                }
              }
            } ?: logger.debug { "Not a gradle module: ${artifact.path}" }
          }
        }
      }
    }
    for (lib in mppLibs) {
      errorSafe {
        onFound(lib)
      }
    }
  }
  
  override fun toString(): String {
    return this::class.simpleName ?: ""
  }
  
  companion object {
    protected val coreCount = Runtime.getRuntime().availableProcessors()
    protected val scannerDispatcher: ExecutorCoroutineDispatcher by lazy {
      Executors.newFixedThreadPool(coreCount).asCoroutineDispatcher()
    }
    
    @JvmStatic
    protected suspend fun <R> parallel(
      dispatcher: CoroutineDispatcher = scannerDispatcher,
      action: suspend CoroutineScope.() -> R,
    ) = coroutineScope {
      (0 until coreCount).map {
        async(dispatcher) {
          errorSafe { action() }
        }
      }
    }
  }
}
