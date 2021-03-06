package org.slug.output

import org.graphstream.graph.Graph
import org.slug.util.ResourceHelper
import java.io.File

object DisplayHelper {
  private fun loadDefaultCSS(): String = loadCSS("style.css")

  private fun loadCSS(styleFile: String): String = ResourceHelper.readResourceFile(styleFile)

  fun loadCSSConfig(styleFile: String): String {
    val css = when {
      !styleFile.isNullOrEmpty() -> loadCSS(styleFile)
      else -> loadDefaultCSS()
    }
    return css
  }
}

fun display(graph: Graph) {
  graph.display()
  Thread.sleep(1000)
  graph.addAttribute("ui.screenshot", "samples" + File.separator + graph.id + "_screenshot.png")
}
