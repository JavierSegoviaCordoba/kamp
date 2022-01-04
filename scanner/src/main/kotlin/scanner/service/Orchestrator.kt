package scanner.service

import kamp.domain.MavenArtifact
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.json.Json
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.instanceOrNull
import scanner.util.LoggerDelegate
import scanner.util.buildTextFile
import scanner.util.buildTomlFile
import scanner.util.supervisedLaunch

class Orchestrator(override val di: DI) : DIAware {
  private val logger by LoggerDelegate()
  private val json by di.instance<Json>()

  suspend fun run(
    scanner: String,
    rootArtefactsFilter: Set<String>? = null,
    rootArtefactsExcludeFilter: Set<String>? = null
  ) {
    println("scanner: $scanner")
    println("filters: $rootArtefactsFilter")
    println("excludeFilters: $rootArtefactsExcludeFilter")

    val scannerService = di.direct.instanceOrNull<MavenScannerService<*>>(scanner)

    scannerService?.let {
      logger.info("Scanning repository: $scanner")

      val duration = measureTimeMillis {
        coroutineScope {
          supervisedLaunch {
            logger.info("Starting $scanner scan")
            val count =
              scanRepo(scanner, scannerService, rootArtefactsFilter, rootArtefactsExcludeFilter)
            logger.info(
              "Found $count kotlin modules with gradle metadata in $scanner repository " +
                "filtered by ${rootArtefactsFilter ?: setOf()}, " +
                "explicitly excluding ${rootArtefactsExcludeFilter ?: setOf()}"
            )
          }
        }
      }
      logger.info(
        "Finished scanning $scanner in ${(duration/1000)/60} minutes"
      )
    }
      ?: logger.error("ScannerService for $scanner not found")
  }

  private suspend fun scanRepo(
    repo: String,
    scanner: MavenScannerService<*>,
    rootArtefactsFilter: Set<String>? = null,
    rootArtefactsExcludeFilter: Set<String>? = null
  ): Int {
    var count = 0

    val libs = mutableListOf<MavenArtifact>()

    coroutineScope {
      scanner
        .scanKotlinLibraries(rootArtefactsFilter, rootArtefactsExcludeFilter)
        .buffer()
        .collect { lib: MavenArtifact ->
          supervisedLaunch {
            count++
            libs.add(lib)
          }
        }
    }

    buildTextFile(repo, libs)
    buildTomlFile(repo, libs)

    scanner.close()
    return count
  }
}
