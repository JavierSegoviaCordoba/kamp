package scanner.util

import java.io.File
import kamp.domain.MavenArtifact

fun buildTextFile(repo: String, libs: List<MavenArtifact>) {

  val libsLines = libs.map { lib -> lib.path }.distinct().sorted()

  val fileName =
    repo +
      "-" +
      libs.first().group.substringBefore(" =").cleanHyphen() +
      "_" +
      libs.last().group.substringBefore(" =").cleanHyphen()

  File("build/catalogs/$fileName.libs.versions.txt").apply {
    parentFile.mkdirs()
    createNewFile()

    writeText(libsLines.joinToString("\n"))
  }
}
