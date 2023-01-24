package part2_lowlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCode, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

object LowLevelApi extends App {

  implicit val system = ActorSystem("LowLevelServerApi")
  implicit val materializer = ActorMaterializer()

  import scala.concurrent.duration._
  import system.dispatcher

  //  val serverSource = Http().bind("localhost", 8080)
  //  val connectionSink = Sink.foreach[IncomingConnection] { connection =>
  //    println(s"Accepted incoming connection from: ${connection.remoteAddress}")
  //  }
  //
  //  val serverBindingFuture = serverSource.to(connectionSink).run()
  //
  //  serverBindingFuture.onComplete {
  //    case Success(binding) =>
  //      println("Server binding successful.")
  //      binding //.unbind() //keeps existing connections alive
  //        .terminate(2 seconds) // terminates existing connections
  //    case Failure(exception) => println(s"Server binding failed. $exception")
  //  }

  /*
    Method 1: synchronously serve HTTP responses
   */

  val requestHandler: HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, _, _, _, _) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPS!  The resource can't be found.
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httSyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithSyncHandler(requestHandler)
  }

  //  Http().bind("localhost", 8080).runWith(httSyncConnectionHandler)
  //shorthand version
  Http().bindAndHandleSync(requestHandler, "localhost", 8080)

  /*
    Method 2: serve back HTTP response ASYNCHRONOUSLY
   */

  val asyncRequestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) => // method, URI, HTTP headers, content and the protocol (HTTP1.1/HTTP2.0)
      Future(HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        )
      ))
    case request: HttpRequest =>
      request.discardEntityBytes()
      // in production have a dedicated execution context to run your futures.
      // running things on system dispatcher will starve your application actor
      // system.
      Future(HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPS!  The resource can't be found.
            | </body>
            |</html>
            |""".stripMargin
        )
      ))
  }

  val httAsyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithAsyncHandler(asyncRequestHandler)
  }

  // stream based version ("manual" version)
  //  Http().bind("localhost", 8081).runWith(httAsyncConnectionHandler)
  // shorthand version
  //  Http().bindAndHandleAsync(asyncRequestHandler, "localhost", 8081)

  /*
    Method 3: async via Akka streams
   */
  val streamBasedRequestHandler: Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), _, _, _) => // method, URI, HTTP headers, content and the protocol (HTTP1.1/HTTP2.0)
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        ))
    case request: HttpRequest =>
      request.discardEntityBytes()
      // in production have a dedicated execution context to run your futures.
      // running things on system dispatcher will starve your application actor
      // system.
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPS!  The resource can't be found.
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  // manual version
  //  Http().bind("localhost", 8082).runForeach { connection =>
  //    connection.handleWith(streamBasedRequestHandler)
  //  }
  // shorthand version
  Http().bindAndHandle(streamBasedRequestHandler, "localhost", 8082)

}
