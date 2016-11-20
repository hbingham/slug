package org.slug

import org.graphstream.graph.Graph
import org.graphstream.graph.implementations.SingleGraph
import org.slug.output.DisplayHelper
import org.slug.output.generateDotFile


class Main {

    companion object {
        val config = Config.fromConfig("default.properties")

        @JvmStatic fun main(args: Array<String>) {
            val css = DisplayHelper().loadCSS()

//            generator(css, simpleArchitecture())
//            generator(css, simple3Tier())
//            generator(css, multipleLinks())
//            generator(css, e2e())
//            generator(css, e2eMultipleApps())

            val architecture = multiService()
            val serviceGraphs = architecture.generators().map { microservice -> generator(css, microservice) }

            val XTalks = architecture.crossTalks()
            val crossTalks = CrossTalkGenerator().addCrossTalk(serviceGraphs, XTalks)
            serviceGraphs.plus(crossTalks)
                    .forEach { graph ->  display(graph); printDotFile(graph) }
        }

        fun generator(css: String, generator: MicroserviceGenerator): SingleGraph {
            val name = generator.architecture.identifier
            val graph = SingleGraph(name)

            System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer")

            graph.addAttribute("ui.stylesheet", css)
            generator.addSink(graph)
            generator.begin()
            generator.end()

            graph.addAttribute("ui.antialias")
            graph.addAttribute("ui.quality")

            return graph
        }

        fun display(graph: Graph) {
            if (config.getBooleanProperty("display.swing")) {
                graph.display()
                Thread.sleep(1000)
                graph.addAttribute("ui.screenshot", "samples/" + graph.id + "_screenshot.png")
            }
        }

        fun printDotFile(graph: Graph) {
            if (config.getBooleanProperty("display.dot")) generateDotFile(graph)
        }
    }
}
