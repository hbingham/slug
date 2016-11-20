package org.slug

import org.graphstream.algorithm.generator.Generator
import org.graphstream.stream.SourceBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slug.core.Component
import org.slug.core.Component.DiscoverableComponent
import org.slug.core.Component.SimpleComponent
import org.slug.core.Layer
import org.slug.core.Microservice
import org.slug.core.validateSize

class MicroserviceGenerator(val architecture: Microservice) : SourceBase(), Generator {
    var logger: Logger? = LoggerFactory.getLogger(javaClass)
    val separator: String = "->"
    val node_separator: String = "_"
    var createdNodes: Sequence<String> = emptySequence()
    var createdEdges: Sequence<String> = emptySequence()

    override fun end() {
    }

    override fun begin() {
        addLayer()
    }

    override fun nextEvents(): Boolean {
        return true
    }

    private fun addLayer() {

        if (!architecture.validateSize()) {
            // this isn't a real architecture, it has just one layer.
            return
        }

        for ((first, second) in
        layerZipper(architecture.layers)) {
            if (first.component.type.identifier == second.component.type.identifier) {
                addComponent(second.component, second.spatialRedundancy)
            } else {
                createLinkLayers(first.component, first.spatialRedundancy, second.component, second.spatialRedundancy)
            }
        }
    }

    private fun createLinkLayers(firstLayerComponent: Component, firstLayerRedundancy: Int, secondLayerComponent: Component, secondLayerRedundancy: Int) {

        val froms = addComponent(firstLayerComponent, firstLayerRedundancy)
        val tos = addComponent(secondLayerComponent, secondLayerRedundancy)
        createLink(firstLayerComponent, froms, secondLayerRedundancy, tos)
    }

    private fun addComponent(component: Component, redundancy: Int): Sequence<String> {
        var nodes = emptySequence<String>()

        when (component) {
            is SimpleComponent -> {
                for (r in 1..redundancy) {
                    val nodeIdentifier = createIdentifier(component.type.identifier, r)
                    nodes = nodes.plus(nodeIdentifier)
                    createNode(nodeIdentifier)
                }
            }
            is DiscoverableComponent -> {
                val nodeIdentifier = component.connection.via.identifier
                createNode(nodeIdentifier)
                (1..redundancy).forEach { r ->
                    val from = createIdentifier(component.type.identifier, r)
                    nodes = nodes.plus(from)
                    createNode(from)
                    createEdge(component, from)
                }
                createNode(component.connection.to.identifier)
                createEdge(component)
            }
        }
        return nodes
    }

    private fun createLink(firstLayerComponent: Component, froms: Sequence<String>, secondLayerRedundancy: Int, tos: Sequence<String>) {
        when (firstLayerComponent) {
            is SimpleComponent -> {
                for (from in froms) {
                    for (to in tos.take(secondLayerRedundancy)) {
                        createEdge(from, to)
                    }
                }
            }
            is DiscoverableComponent -> {
                for ((from, to) in froms.zip(tos)) {
                    createEdge(from, to)
                }
            }
        }
    }

    fun createNode(nodeIdentifier: String) {
        if (!createdNodes.contains(nodeIdentifier)) {
            logger?.debug("creating node " + nodeIdentifier)
            sendNodeAdded(sourceId, nodeIdentifier)
            sendNodeAttributeAdded(sourceId, nodeIdentifier, "ui.label", nodeIdentifier)
            createdNodes = createdNodes.plus(nodeIdentifier)
        }
    }

    fun createEdge(from: String, to: String) {
        val edgeId = from + separator + to
        val reverseEdgeId = to + separator + from
        if (!(createdEdges.contains(edgeId) || createdEdges.contains(reverseEdgeId))) {
            logger?.debug("creating edge " + edgeId)
            sendEdgeAdded(sourceId, edgeId, from, to, true)
            sendEdgeAttributeAdded(sourceId, edgeId, "ui.style", "shape:cubic-curve; fill-color: rgb(255,0,160), rgb(0,255,1);")
            createdEdges = createdEdges.plus(edgeId)
        }
    }

    fun createEdge(component: DiscoverableComponent, nodeIdentifier: String) {
        createEdge(nodeIdentifier, component.connection.via.identifier)
    }

    fun createEdge(component: DiscoverableComponent) {
        createEdge(component.connection.via.identifier, component.connection.to.identifier)
    }

    fun createIdentifier(identifier: String, append: Int) = identifier + node_separator + append

    fun layerZipper(sequence: Sequence<Layer>) =
            if (sequence.count() == 2) sequenceOf(Pair(sequence.first(), sequence.last()))
            else sequence.zip(sequence.drop(1))

}