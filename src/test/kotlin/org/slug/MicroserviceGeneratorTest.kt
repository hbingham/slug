package org.slug

import org.graphstream.graph.implementations.SingleGraph
import org.junit.Assert.assertEquals
import org.junit.Test
import org.slug.core.Component.DiscoverableComponent
import org.slug.core.Component.SimpleComponent
import org.slug.core.InfrastructureType.*
import org.slug.core.LayerConnection.*
import org.slug.generators.MicroserviceGenerator
import org.slug.factories.Cranks
import org.slug.factories.Infrastructure
import org.slug.factories.MicroserviceFactory


class MicroserviceGeneratorTest {

    val factory = MicroserviceFactory(Cranks("dense", "medium"), Infrastructure.loadInfrastructureConfig("infrastructure.json"))
    @Test
    fun shouldAddAllTheComponents() {

        val generator = MicroserviceGenerator(factory.simple())
        val graph = SingleGraph("First")
        generator.addSink(graph)
        generator.begin()
        generator.end()

        assertEquals(18, graph.nodeCount)
        assertEquals(35, graph.edgeCount)

    }

    @Test
    fun shouldCreateLinksBetweenComponents() {
        val generator = MicroserviceGenerator(factory.simple3Tier())
        val graph = SingleGraph("First")
        generator.addSink(graph)
        generator.begin()
        generator.end()

        assertEquals(19, graph.nodeCount)
        assertEquals(37, graph.edgeCount)
    }

    @Test
    fun canCreateMultipleLayerLinksFromAComponent() {

        val generator = MicroserviceGenerator(factory.multipleLinks())
        val graph = SingleGraph("First")
        generator.addSink(graph)
        generator.begin()
        generator.end()

        assertEquals(25, graph.nodeCount)
        assertEquals(52, graph.edgeCount)

    }

    @Test
    fun e2eArchitecture() {

        val generator = MicroserviceGenerator(factory.e2e())
        val graph = SingleGraph("First")
        generator.addSink(graph)
        generator.begin()
        generator.end()

        assertEquals(27, graph.nodeCount)
        assertEquals(54, graph.edgeCount)

    }

    @Test
    fun e2eArchitectureWithMultipleApps() {

        val generator = MicroserviceGenerator(factory.e2eMultipleApps())
        val graph = SingleGraph("First")
        generator.addSink(graph)
        generator.begin()
        generator.end()

        assertEquals(37, graph.nodeCount)
        assertEquals(74, graph.edgeCount)

    }

    @Test
    fun e2eWithCache() {

        val generator = MicroserviceGenerator(factory.e2eWithCache())
        val graph = SingleGraph("First")
        generator.addSink(graph)
        generator.begin()
        generator.end()

        assertEquals(37, graph.nodeCount)
        assertEquals(154, graph.edgeCount)

    }
}