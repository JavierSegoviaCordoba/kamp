package app.config

import app.service.LibraryService
import app.service.path
import app.util.PublicEnv
import app.util.inject
import app.util.page
import app.util.pageSize
import app.util.search
import app.util.targets
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.default
import io.ktor.http.content.defaultResource
import io.ktor.http.content.files
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing


fun Application.routing() = routing {
  libraries()
  get("/application.env") {
    call.respondText("$PublicEnv")
  }
  staticContent()
}

private fun Routing.libraries() = route(LibraryService.path) {
  get {
    val service by inject<LibraryService>()
    call.respond(service.getAll(call.request.page, call.request.pageSize, call.request.search, call.request.targets))
  }
  get("/count") {
    val service by inject<LibraryService>()
    call.respond(service.getCount(call.request.search, call.request.targets))
  }
  
  authenticate {
    post {
      val service by inject<LibraryService>()
      val entity = service.create(call.receive())
      call.respond(HttpStatusCode.Created, entity)
    }
  }
}

private fun Routing.staticContent() = static {
  val folder = "WEB-INF"
  val index = "$folder/index.html"
  files(folder)
  default(index)
  resources(folder)
  defaultResource(index)
}
