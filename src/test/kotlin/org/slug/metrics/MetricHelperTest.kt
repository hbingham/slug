package org.slug.metrics

import org.graphstream.graph.implementations.SingleGraph
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slug.factories.Cranks
import org.slug.factories.Infrastructure
import org.slug.factories.MicroserviceFactory
import org.slug.generators.MicroserviceGenerator

class MetricHelperTest {

  val densityMap = mapOf("sparse" to 4, "dense" to 10, "hyperdense" to 15)
  val replicationMap = mapOf("minimal" to 3, "medium" to 5, "high" to 7)
  val factory = MicroserviceFactory(Cranks("dense", "medium"), Infrastructure.loadInfrastructureConfig("infrastructure.json"), densityMap, replicationMap)

  @Test
  fun shouldCalculateMetrics() {

    val generator = MicroserviceGenerator(factory.simple())
    val graph = SingleGraph("First")
    generator.addSink(graph)
    generator.begin()
    generator.end()

    val measurements = measurements(sequenceOf(graph))
    val measurement = measurements.first()

    assertEquals(SubPlot(3, 4, 1), measurement.subPlot)
    assertEquals("Graph Id", measurement.chartParams.xLabel)
  }

  @Test
  fun shouldAggregateMetrics() {
    val generator = MicroserviceGenerator(factory.simple())
    val simple = SingleGraph("simple")
    generator.addSink(simple)
    generator.begin()
    generator.end()

    val otherGenerator = MicroserviceGenerator(factory.e2e())
    val e2e = SingleGraph("e2e")
    otherGenerator.addSink(e2e)
    otherGenerator.begin()
    otherGenerator.end()

    val measurements = measurements(sequenceOf(simple, e2e, simple, e2e))

    val aggregateMetrics = combineMetrics(measurements).first()

    assertEquals(4, aggregateMetrics.chartData.xValues.size)
    assertEquals(4, aggregateMetrics.chartData.yValues.size)
    assertEquals(0.23, aggregateMetrics.chartData.yValues.first(), 0.0)
    assertEquals(0.15, aggregateMetrics.chartData.yValues.last(), 0.0)

  }
}