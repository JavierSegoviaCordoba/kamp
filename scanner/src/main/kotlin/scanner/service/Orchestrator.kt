package scanner.service

import kamp.domain.MavenArtifact
import kotlin.time.measureTime
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
    val scannerService = di.direct.instanceOrNull<MavenScannerService<*>>(scanner)

    scannerService?.let {
      logger.info("Scanning repository: $scanner")

      val duration = measureTime {
        coroutineScope {
          supervisedLaunch {
            logger.info("Starting $scanner scan")
            val count = scanRepo(scannerService, rootArtefactsFilter, rootArtefactsExcludeFilter)
            logger.info(
              "Found $count kotlin modules with gradle metadata in $scanner repository " +
                "filtered by ${rootArtefactsFilter ?: setOf()}, " +
                "explicitly excluding ${rootArtefactsExcludeFilter ?: setOf()}"
            )
          }
        }
      }
      logger.info(
        "Finished scanning $scanner in ${
          duration.toComponents { hours, minutes, seconds, nanoseconds ->
            "${hours}h ${minutes}m ${seconds}.${nanoseconds}s"
          }
        }")
    }
      ?: logger.error("ScannerService for $scanner not found")
  }

  private suspend fun scanRepo(
    scanner: MavenScannerService<*>,
    rootArtefactsFilter: Set<String>? = null,
    rootArtefactsExcludeFilter: Set<String>? = null
  ): Int {
    var count = 0

    val libs = mutableListOf<MavenArtifact>()
    coroutineScope {
      with(scanner) { scanMavenArtefacts(rootArtefactsFilter, rootArtefactsExcludeFilter) }
        .buffer()
        .collect { lib: MavenArtifact ->
          supervisedLaunch {
            count++
            libs.add(lib)
          }
        }
    }

    buildTomlFile(libs)

    scanner.close()
    return count
  }
}
