@file:OptIn(ExperimentalStdlibApi::class)

package scanner.util

import java.io.File
import kamp.domain.MavenArtifact

fun buildTomlFile(libs: List<MavenArtifact>) {

  val versionsLines = libs.map { lib -> buildVersionLib(lib) }.sorted()
  val libsLines = libs.map { lib -> buildLibLine(lib) }.sorted()

  File(
    "build/catalogs/${libsLines.map { it.first() }.distinct().joinToString("")}.libs.versions.toml"
  )
    .apply {
      parentFile.mkdirs()
      if (!exists()) createNewFile()

      writeText(
        buildList {
          add("[versions]")
          addAll(versionsLines)
          add("")
          add("[libraries]")
          addAll(libsLines)
        }
          .joinToString("\n"))
    }
}

private fun buildVersionLib(lib: MavenArtifact): String {
  return """${buildAlias(lib).cleanHyphen()} = "${lib.latestVersion}""""
}

private fun buildLibLine(lib: MavenArtifact): String {

  val group = """group = "${lib.group}""""
  val name = """name = "${lib.name}""""
  val versionRef = """version.ref = "${buildAlias(lib).cleanHyphen()}""""

  return """${buildAlias(lib)} = { $group, $name, $versionRef }"""
}

private fun buildAlias(lib: MavenArtifact): String {
  val alias =
    lib.group.replace(".", "-").replace(":", "-") +
      "-" +
      lib.name.cleanHyphen().replace(".", "_").replace(":", "_")

  with(alias) {
    return if (endsWith("version", true) ||
      endsWith("versions", true) ||
      endsWith("bundle", true) ||
      endsWith("bundles", true)
    ) {
      alias + "_"
    } else {
      alias
    }
  }
}

private fun String.cleanHyphen(firstLowerCase: Boolean = true): String =
  this.split('-')
    .joinToString("", transform = String::capitalize)
    .mapIndexed { index: Int, char: Char ->
      if (index == 0 && firstLowerCase) char.toLowerCase() else char
    }
    .joinToString("")
