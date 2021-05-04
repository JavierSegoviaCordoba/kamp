package scanner

import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import scanner.config.di
import scanner.service.Orchestrator

abstract class ScannerTask : DefaultTask() {

  abstract var repository: String
    @Input get
    @Option(option = "repository", description = "Maven repository to scan") set

  abstract var from: String
    @Input get
    @Option(option = "from", description = "Index from where scanner should start the scan") set

  abstract var to: String
    @Input get
    @Option(option = "to", description = "Index from where scanner should end the scan") set

  @TaskAction
  fun run() {
    runBlocking {
      val rangeFilter = (from.first()..to.first()).map(Char::toString).toSet()
      val filters = ((emptyList<String>().toSet()) + rangeFilter).takeIf { it.isNotEmpty() }
      val excludeFilters = emptyList<String>().toSet().takeIf { it.isNotEmpty() }

      Orchestrator(di).run(repository, filters, excludeFilters)
    }
  }
}
