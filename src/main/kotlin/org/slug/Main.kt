package org.slug

import org.graphstream.graph.Graph
import org.slug.core.InfrastructureType
import org.slug.factories.ArchitectureFactory.buildArchitectures
import org.slug.factories.ArchitectureFactory.fromMicroservices
import org.slug.factories.Cranks
import org.slug.factories.Infrastructure.Companion.loadInfrastructureConfig
import org.slug.factories.InfrastructureFactory.Companion.create
import org.slug.factories.MicroserviceFactory
import org.slug.factories.buildServiceGraphs
import org.slug.factories.buildServices
import org.slug.generators.LayerGenerator
import org.slug.generators.MicroserviceGenerator
import org.slug.log.LogGenerator
import org.slug.metrics.*
import org.slug.output.DisplayHelper
import org.slug.output.DotConfiguration
import org.slug.output.display
import org.slug.output.generateDotFile
import org.slug.util.Config
import org.slug.util.ResourceHelper.readResourceAsMap
import org.slug.util.ResourceHelper.readTemplates
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.runAsync


class Main {

  companion object {
    val config = Config.fromConfig("default.properties")

    @JvmStatic fun main(args: Array<String>) {

      val outputFromConfig = config.getProperty("outputDirectory")
      val traceDirectory = config.getProperty("traceDirectory")

      val outputDirectory = if (outputFromConfig.isNullOrEmpty()) "samples" else outputFromConfig

      val file = File(outputDirectory)
      if (!file.exists()) file.mkdirs()

      val styleFile = config.getProperty("style")
      val css = DisplayHelper.loadCSSConfig(styleFile)

      val replicationMap = readResourceAsMap("replication.properties")
      val densityMap = readResourceAsMap("density.properties")

      val infrastructure = loadInfrastructureConfig()

      val crank = getCranks()
      val calculateMetrics = config.getBooleanProperty("metrics")
      val iterations = config.getIntegerProperty("iterations")
      var futures = emptyArray<CompletableFuture<Void>>()
      var aggregateMetrics = emptySequence<Metric>()

      (1..iterations).forEach { iteration ->

        val dotDirectory = File.separator + "i_" + iteration
        val layerDirectory = dotDirectory + "_l"
        val services = buildServices(MicroserviceFactory(crank, infrastructure, densityMap, replicationMap))
        val graphs = buildServiceGraphs(services, css, MicroserviceGenerator::class.java)
        val layeredGraphs = buildServiceGraphs(services, css, LayerGenerator::class.java)
        val architectures = fromMicroservices(services, create<InfrastructureType.ServiceRegistry>(infrastructure))
        val crossTalks = buildArchitectures(architectures, css, MicroserviceGenerator::class.java)

        if (calculateMetrics) {
          futures = futures.plus(runAsync {
            val metrics = measurements(graphs)
            aggregateMetrics = aggregateMetrics.plus(metrics)
            display(graphs, DotConfiguration(outputDirectory, dotDirectory))
          })
          futures = futures.plus(runAsync {
            val metrics = measurements(layeredGraphs)
            aggregateMetrics = aggregateMetrics.plus(metrics)
            display(layeredGraphs, DotConfiguration(outputDirectory, layerDirectory))

          })
          futures = futures.plus(runAsync {
            val metrics = measurements(crossTalks)
            aggregateMetrics = aggregateMetrics.plus(metrics)
            display(crossTalks, DotConfiguration(outputDirectory, dotDirectory))
          })
        }

        writeTrace(graphs, traceDirectory + File.separator + "i_" + iteration)

      }

      CompletableFuture.allOf(*futures).get()
      val trace = config.getBooleanProperty("trace")
      if (trace)
        printMetrics(combineMetrics(aggregateMetrics), MetricConfig(outputDirectory, "metrics"))
    }

    private fun writeTrace(graphs: Sequence<Graph>, traceDirectory: String) {
      val traceFile = File(traceDirectory)
      if (!traceFile.exists()) traceFile.mkdirs()
      val logGenerator = LogGenerator(readTemplates())

      graphs.forEach { g ->
        val trace = logGenerator.tracePath(g).joinToString("\n")
        Files.write(Paths.get(traceDirectory + File.separator + g.id + ".trc"), trace.toByteArray())
      }
    }

    private fun display(graphs: Sequence<Graph>, dotConfiguration: DotConfiguration) {
      graphs
        .forEach { graph ->
          if (config.getBooleanProperty("display.swing")) display(graph)
          if (config.getBooleanProperty("display.dot")) generateDotFile(graph, dotConfiguration)
        }
    }

    private fun getCranks(): Cranks {
      val serviceDensity = config.getProperty("densityFromDistribution")
      val replicationFactor = config.getProperty("replication")
      val powerLaw = config.getBooleanProperty("powerlaw")
      return Cranks(serviceDensity, replicationFactor, powerLaw)
    }

  }
}
