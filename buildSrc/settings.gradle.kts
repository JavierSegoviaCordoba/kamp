import de.fayard.refreshVersions.bootstrapRefreshVersionsForBuildSrc
import de.fayard.refreshVersions.migrateRefreshVersionsIfNeeded

buildscript {
  repositories { gradlePluginPortal() }
  dependencies {
    classpath("de.fayard.refreshVersions:refreshVersions:0.9.7")
////                                         # available:0.10.0")
////                                         # available:0.10.1")
////                                         # available:0.11.0")
////                                         # available:0.20.0")
  }
}

migrateRefreshVersionsIfNeeded("0.9.7") // Will be automatically removed by refreshVersions when upgraded to the latest version.

bootstrapRefreshVersionsForBuildSrc()
