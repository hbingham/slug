package org.slug.generators

import org.graphstream.algorithm.generator.Generator
import org.graphstream.stream.SourceBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slug.core.*
import org.slug.core.Component.DiscoverableComponent
import org.slug.core.Component.SimpleComponent
import org.slug.output.DisplayConstants.LABEL
import org.slug.output.DisplayConstants.STYLE
import org.slug.output.GraphConstants.EDGE_SEPARATOR
import org.slug.output.GraphConstants.EDGE_STYLE
import org.slug.output.GraphConstants.SEPARATOR

open class MicroserviceGenerator(val architecture: Microservice) : SourceBase(), Generator {
  var logger: Logger? = LoggerFactory.getLogger(javaClass)
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
      when {
        areEqual(first, second) -> addComponent(second.component, second.spatialRedundancy)
        else -> createLinkLayers(first.component, first.spatialRedundancy, second.component, second.spatialRedundancy)
      }
    }
  }

  private fun createLinkLayers(firstLayerComponent: Component, firstLayerRedundancy: Int, secondLayerComponent: Component, secondLayerRedundancy: Int) {
    val froms = addComponent(firstLayerComponent, firstLayerRedundancy)
    val tos = addComponent(secondLayerComponent, secondLayerRedundancy)
    createLink(firstLayerComponent, froms, secondLayerRedundancy, tos)
  }

  private fun addComponent(component: Component, redundancy: Int): Sequence<String> {
    logger?.debug("adding component " + component)
    var nodes = emptySequence<String>()

    when (component) {
      is SimpleComponent -> {
        (1..redundancy).forEach { r ->
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
        when (component.connection.to) {
          is InfrastructureType.Database -> {
            val database = component.connection.to
            (1..database.replicationFactor).forEach { d ->
              val to = createIdentifier(database.identifier, d)
              createNode(to)
              createEdge(component.connection.via.identifier, to)
            }
          }
          else -> {
            createNode(component.connection.to.identifier)
            createEdge(component)
          }
        }
      }
    }
    return nodes
  }

  private fun createLink(firstLayerComponent: Component, froms: Sequence<String>, secondLayerRedundancy: Int, tos: Sequence<String>) =
      when (firstLayerComponent) {
        is SimpleComponent -> {
          logger?.debug("creating link for simple component " + firstLayerComponent)
          froms.forEach { from ->
            tos.take(secondLayerRedundancy).forEach { to -> createEdge(from, to) }
          }
        }
        is DiscoverableComponent -> {
          logger?.debug("creating link for discoverable component " + firstLayerComponent)
          for ((from, to) in froms.zip(tos)) {
            createEdge(from, to)
          }
        }
      }

  fun createNode(nodeIdentifier: String) {
    if (!createdNodes.contains(nodeIdentifier)) {
      logger?.debug("creating node " + nodeIdentifier)
      sendNodeAdded(sourceId, nodeIdentifier)
      sendNodeAttributeAdded(sourceId, nodeIdentifier, LABEL, nodeIdentifier)
      createdNodes = createdNodes.plus(nodeIdentifier)
    }
  }

  fun createEdge(from: String, to: String) {
    val edgeId = from + EDGE_SEPARATOR + to
    val reverseEdgeId = to + EDGE_SEPARATOR + from
    if (!(createdEdges.contains(edgeId) || createdEdges.contains(reverseEdgeId))) {
      logger?.debug("creating edge " + edgeId)
      sendEdgeAdded(sourceId, edgeId, from, to, true)
      sendEdgeAttributeAdded(sourceId, edgeId, STYLE, EDGE_STYLE)
      createdEdges = createdEdges.plus(edgeId)
    }
  }

  fun createEdge(component: DiscoverableComponent, nodeIdentifier: String) =
      createEdge(nodeIdentifier, component.connection.via.identifier)

  fun createEdge(component: DiscoverableComponent) =
      createEdge(component.connection.via.identifier, component.connection.to.identifier)

  fun createIdentifier(identifier: String, append: Int) =
      identifier + SEPARATOR + append

  fun areEqual(first: Layer, second: Layer) =
      first.component.type.identifier == second.component.type.identifier

  fun layerZipper(sequence: Sequence<Layer>) =
      if (sequence.count() == 2) sequenceOf(Pair(sequence.first(), sequence.last()))
      else sequence.zip(sequence.drop(1))

}
