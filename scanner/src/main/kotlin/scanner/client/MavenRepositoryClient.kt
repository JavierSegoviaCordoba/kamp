package scanner.client

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.utils.io.core.Closeable
import kamp.domain.MavenArtifact
import kamp.domain.MavenArtifactImpl
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jsoup.nodes.Document
import scanner.domain.GradleModule
import scanner.util.LoggerDelegate
import scanner.util.asDocument
import scanner.util.supervisedAsync

abstract class MavenRepositoryClient<A : MavenArtifact>(
  private val defaultRepositoryRootUrl: String,
) : Closeable {
  protected abstract fun parsePage(page: Document): List<String>?
  protected abstract val client: HttpClient
  protected abstract val json: Json
  private val logger by LoggerDelegate()

  private val A.mavenModuleRootUrl: String
    get() = "$defaultRepositoryRootUrl/${group.replace(".", "/")}/$name"
  private val A.mavenModuleVersionUrl: String
    get() = "$mavenModuleRootUrl/$releaseVersion"

  suspend fun getArtifactDetails(pathToMavenMetadata: String): MavenArtifactImpl? =
    coroutineScope {
      val artifact = supervisedAsync {
        val url = "$defaultRepositoryRootUrl$pathToMavenMetadata"
        val pom = client.get<String>(url).asDocument()
        val doc = pom.getElementsByTag("metadata").first() ?: error("doc can't be null")
        try {
          MavenArtifactImpl(
            group = doc.selectFirst("groupId")!!.text(),
            name = doc.selectFirst("artifactId")!!.text(),
            latestVersion = doc.selectFirst("versioning>latest")?.text() ?: doc.selectFirst("version")!!.text(),
            releaseVersion = doc.selectFirst("versioning>release")?.text(),
            versions = doc.selectFirst("versioning>versions")?.children()?.map { v -> v.text() },
            lastUpdated = doc.selectFirst("versioning>lastUpdated")?.text()?.toLongOrNull(),
          )
        } catch (e: Exception) {
          if (doc.selectFirst("plugins") == null) {
            logger.warn("Unable to parse maven-metadata.xml from $url")
          }
          null
        }
      }

      artifact.await()
    }

  suspend fun getGradleModule(artifact: A): GradleModule? = coroutineScope {
    supervisedAsync {
      val module = client.get<String>("${artifact.mavenModuleVersionUrl}/${artifact.name}-${artifact.releaseVersion}.module")
      json.decodeFromString<GradleModule>(module)
    }.await()
  }

  suspend fun getMavenPom(artifact: A): Document? = coroutineScope {
    supervisedAsync {
      client.get<String>("${artifact.mavenModuleVersionUrl}/${artifact.name}-${artifact.releaseVersion}.pom").asDocument()
    }.await()
  }

  suspend fun listRepositoryPath(path: String): List<RepoItem>? = coroutineScope {
    supervisedAsync {
      client.get<String>("$defaultRepositoryRootUrl${path.removeSuffix("/")}/").let { str ->
        parsePage(str.asDocument())?.map { RepoItem(it, path) }
      }
    }.await()
  }

  override fun close() = client.close()

  class RepoItem(val value: String, path: String) {
    val parentPath = "/${path.removeSuffix("/").removePrefix("/")}"
    val path = "${if (parentPath == "/") "" else parentPath}/$value".removeSuffix("/")
    val isDirectory = value.endsWith("/")
    val isFile = !isDirectory

    override fun toString() = value
  }
}
