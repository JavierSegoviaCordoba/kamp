@file:OptIn(ExperimentalStdlibApi::class)

package scanner.util

import java.io.File
import kamp.domain.MavenArtifact

fun buildTomlFile(repo: String, libs: List<MavenArtifact>) {

  val versionsLines = libs.map { lib -> buildVersionLib(lib) }.distinct().sorted()
  val libsLines = libs.map { lib -> buildLibLine(lib) }.distinct().sorted()

  val fileName =
    repo +
      "-" +
      libsLines.first().substringBefore(" =").cleanHyphen() +
      "_" +
      libsLines.last().substringBefore(" =").cleanHyphen()

  File("build/catalogs/$fileName.libs.versions.toml").apply {
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
        .joinToString("\n")
    )
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

  val domain = lib.group.split(".").take(2).joinToString(".")
  val groupLessDomain: String =
    lib
      .group
      .replace("$domain.", "")
      .replace(".", "-")
      .replace("_", "-")
      .replace(":", "-")
      .cleanHyphen()

  val group = "${domain.replace(".", "-")}-$groupLessDomain"
  val name =
    lib
      .name
      .replace("$domain.", "")
      .replace(".", "-")
      .replace("_", "-")
      .replace(":", "-")
      .cleanHyphen(firstLowerCase = lib.group.split(".").size == 2)

  val alias = "$group$name"

  return alias.run {
    if (listOf(
        "version",
        "versions",
        "bundle",
        "bundles",
        "plugin",
        "plugins",
      )
        .any { bannedKey -> this.endsWith(bannedKey, ignoreCase = true) }
    ) {
      this + "X"
    } else {
      this
    }
  }
}

private fun String.cleanHyphen(firstLowerCase: Boolean = true): String =
  this.split('-').toStringCamelCase(firstLowerCase)

private fun String.cleanUnderscore(firstLowerCase: Boolean = true): String =
  this.split('_').toStringCamelCase(firstLowerCase)

private fun List<String>.toStringCamelCase(firstLowerCase: Boolean = true): String =
  joinToString("", transform = String::capitalize)
    .mapIndexed { index: Int, char: Char ->
      if (index == 0 && firstLowerCase) char.toLowerCase() else char
    }
    .joinToString("")
