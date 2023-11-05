package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.javadsl.server.MissingQueryParamRejection
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{MethodRejection, Rejection, RejectionHandler}

object HandlingRejections extends App {
  implicit val system = ActorSystem("HandlingRejections")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val simpleRoute = path("api" / "myEndpoint") {
    get {
      complete(StatusCodes.OK)
    } ~ parameter('id) { _ =>
      complete(StatusCodes.OK)
    }
  }

  // Rejection handlers
  val badRequestHandler: RejectionHandler = { rejections: Seq[Rejection] =>
    println(s"I have encountered rejections: $rejections")
    Some(complete(StatusCodes.BadRequest))
  }

  val forbiddenHandler: RejectionHandler = { rejections: Seq[Rejection] =>
    println(s"I have encountered rejections: $rejections")
    Some(complete(StatusCodes.Forbidden))
  }

  val simpleRouteWithHandlers = handleRejections(badRequestHandler) { // handle rejections from the top level
    // define server logic inside
    path("api" / "myEndpoint") {
      get {
        complete(StatusCodes.OK)
      } ~ post {
        handleRejections(forbiddenHandler) { // handle rejections WITHIN
          parameter('myParam) { _ =>
            complete(StatusCodes.OK)
          }
        }
      }
    }
  }

  // http GET "localhost:8080/api/myEndpoint"             valid
  // http POST "localhost:8080/api/myEndpoint?myParam=2"  valid
  // http PUT "localhost:8080/api/myEndpoint"             invalid
  // http POST "localhost:8080/api/SomeOtherEndpoint"     invalid, not found, empty rejection list, it was wrong enough to be not found at all
  // RejectionHandler.default: this is the handler by default being used everywhere
  //  Http().bindAndHandle(simpleRouteWithHandlers, "localhost", 8080)

  //
  // val simpleRoute = path("api" / "myEndpoint") {
  //    get {
  //      complete(StatusCodes.OK)
  //    } ~ parameter('id) { _ =>
  //      complete(StatusCodes.OK)
  //    }
  //  }
  // because of the above route skeleton we also define a list of rejections as:
  //              list(method rejection, query param rejection)
  // - first, 'get' defines that the request potentially does not meet the GET requirement
  // - second, 'parameter' defines that the request potentially does not have the correct query
  //
  // so if you have ONE handle {...} with multiple cases of rejections, it will first match the
  // case MethodRejection.  If it matches, it does not go forward to check if the request is
  // also query param rejection.  If the request if correct GET and does not have MethodRejection,
  // it will go on to next and check if it is MissingQueryParamRejection
  //
  // if you have MANY handle {...}, every handle will be tested until we find a handle that
  // contains rejection information.  this way you can handle and look for issues with
  // priority using multiple handles.
  //
  //
  implicit val customRejectionHandler: RejectionHandler = RejectionHandler.newBuilder() handle {
    case m: MissingQueryParamRejection =>
      println(s"I got a query param rejection: $m")
      complete("Rejected query param!")
  } handle {
    case m: MethodRejection =>
      println(s"I got a method rejection: $m")
      complete("Rejected method!")
  } result()

  // having an implicit rejection handler is called sealing a route.
  // no matter what http request you get in your route, you always
  // having a defined action for.

  Http().bindAndHandle(simpleRoute, "localhost", 8080)


}
