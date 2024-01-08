package part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import part4_client.PaymentSystemDomain.PaymentRequest
import spray.json._

import java.util.UUID
import scala.util.{Failure, Success, Try}

object HostLevel extends App with PaymentJsonProtocol {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val poolFlow: Flow[(HttpRequest, Int), (Try[HttpResponse], Int), Http.HostConnectionPool] = Http().cachedHostConnectionPool[Int]("www.google.com")

  Source(1 to 10)
    .map(i => HttpRequest() -> i)
    .via(poolFlow)
    .map {
      case (Success(response), value) =>
        // VERY IMPORTANT
        response.discardEntityBytes()
        s"Request $value has received response: $response"
      case (Failure(ex), value) =>
        s"Request $value has failed: $ex"
    }
  //    .runWith(Sink.foreach[String](println))

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniels-account"),
    CreditCard("1234-1234-4321-4321", "321", "my-awesome-account")
  )

  val paymentRequest = creditCards.map(creditCard => PaymentRequest(creditCard, "rtjvm-store-account", 99))
  val serverHttpRequests = paymentRequest.map(paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    ) -> UUID.randomUUID().toString
  )

  Source(serverHttpRequests)
    // we define [String] because String is what we use as the second value.
    // in this case the random uuid string
    .via(Http().cachedHostConnectionPool[String]("localhost", 8080))
    .runForeach { // (Try[HttpResponse], String)
      case (Success(response@HttpResponse(StatusCodes.Forbidden, _, _, _)), orderId) =>
        println(s"The order ID $orderId was not allowed to proceed: $response ")
      case (Success(response), orderId) =>
        println(s"The order ID $orderId was successful and returned the response: $response")
      // do something with the order ID: dispatch it, send a notification to thw customer, etc
      // one of the main strengths of host level api
      case (Failure(ex), orderId) =>
        println(s"The order ID $orderId could not be completed: $ex ")
    }

  // high volume, low latency requests


}
