package part3_highlevelserver

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}
import akka.stream.ActorMaterializer

import scala.language.postfixOps

object DirectivesBreakdown extends App {
  implicit val system = ActorSystem("DirectivesBreakdown")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  /**
   * Type #1: filtering directives
   */

  val simpleHttpMethodRoute =
    post { // equivalent directives for get, put, patch, delete, head, options
      complete(StatusCodes.Forbidden)
    }

  val simplePathRoute =
    path("about") {
      complete(HttpEntity(
        ContentTypes.`application/json`,
        """
          |<html>
          | <body>
          |   Hello from the about page!
          | </body>
          |</html>
          |""".stripMargin
      ))
    }

  val complexPathRoute =
    path("api" / "myEndpoint") { // use this one
      complete(StatusCodes.OK)
    } // api/myEndpoint

  val dontConfuse =
    path("api/myEndpoint") { // it url encodes the string, use above instead
      complete(StatusCodes.OK)
    }

  val pathEndRoute =
    pathEndOrSingleSlash { // localhost:8080 or localhost:8080/
      complete(StatusCodes.OK)
    }

  /**
   * Type 2: extraction directives
   */

  //GET on /api/item/42
  val pathExtractionRoute = path("api" / "item" / IntNumber) { (itemNumber: Int) =>
    // other directives
    println(s"I've got a number in my path: $itemNumber")
    complete(StatusCodes.OK)
  }

  val pathMultiExtractRoute = path("api" / "order" / IntNumber / IntNumber) { (id, inventory) =>
    println(s"I've got TWO number in my path: $id, $inventory")
    complete(StatusCodes.OK)
  }

  val queryParamExtractionRoute = path("api" / "item") {
    parameter('id.as[Int]) { itemId: Int => // use 'id instead od "id" because it is better for performance on how scala stores them on a special place on the jvm
      println(s"I've extracted the ID as $itemId")
      complete(StatusCodes.OK)
    }
  }

  val extractRequestRoute = path("controlEndpoint") {
    extractRequest { httpRequest: HttpRequest =>
      extractLog { log: LoggingAdapter =>
        log.info(s"I've got the http request: $httpRequest")
        complete(StatusCodes.OK)
      }
      complete(StatusCodes.OK)
    }
  }

//  Http().bindAndHandle(queryParamExtractionRoute, "localhost", 8080)

  /**
   * Type 3: composite directives
   */

  val simpleNestedRoute = path("api" / "item") {
    get {
      complete(StatusCodes.OK)
    }
  }

  val compactSimpleNestedRoute = (path("api" / "item") & get) {
    complete(StatusCodes.OK)
  }

  val compactExtractRequestRoute = (path("controlEndpoint") & extractRequest & extractLog) { (request, log) =>
    log.info(s"I've got the http request: $request")
    complete(StatusCodes.OK)
  }

  // /about and /aboutUs
  val repeatedRoute = path("about") {
    complete(StatusCodes.OK)
  } ~ path("aboutUs") {
    complete(StatusCodes.OK)
  }

  val dryoRoute = (path("about") | path("aboutUs")) {
    complete(StatusCodes.OK)
  }

  // yourblog.com/42 AND yourblog.com?postId=42
  val blogByIdRoute = path(IntNumber) { blogId: Int =>
    //complex server logic
    complete(StatusCodes.OK)
  }

  val blogByQueryParamRoute = parameter("postId".as[Int]) { blogpostId: Int =>
    //complex server logic
    complete(StatusCodes.OK)
  }

  val combinedBlogByIdRoute = (path(IntNumber) | parameter("postId".as[Int])) { blogpostId: Int =>
    //complex server logic
    complete(StatusCodes.OK)
  }

  /**
   * Type 4: "actionable" directives
   */
  val completeOkRoute = complete(StatusCodes.OK)

  val failedRoute = path("notSupported"){
    failWith(new RuntimeException("Unsupported!"))
  }

  val routeWithRejection = path("home"){
    reject
  } ~ path("index") {
    completeOkRoute
  }

  /**
   * Exercise: can you spot the mistake?!
   */
  val getOrPutPath = path("api" / "myEndpoint"){
    get {
      completeOkRoute
    } ~
    post {
      complete(StatusCodes.Forbidden)
    }
  }

  Http().bindAndHandle(getOrPutPath, "localhost", 8080)


}
