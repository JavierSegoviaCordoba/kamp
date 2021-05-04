package app.view.component

import app.util.styled
import app.view.KampComponent
import dev.fritz2.dom.html.A
import dev.fritz2.dom.html.RenderContext

@KampComponent
fun RenderContext.Link(href: String, target: String? = null, block: A.() -> Unit = {}) {
  styled(::div)({
    radius { small }
    border {
      width { none }
    }
    hover {
      background {
        color { lighterGray }
      }
    }
    paddings {
      top { tiny }
      bottom { tiny }
      left { small }
      right { small }
    }
  }) {
    styled(::a)({
      fontSize { normal }
      fontWeight { semiBold }
      color { dark }
    }) {
      href(href)
      target?.let { target(it) }
      block()
    }
  }
}
