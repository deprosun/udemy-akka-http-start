package part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import part4_client.PaymentSystemDomain.PaymentRequest
import spray.json._

import scala.util.{Failure, Success}

object RequestLevel extends App with PaymentJsonProtocol {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val responseFuture = Http().singleRequest(HttpRequest(uri = "http://www.google.com"))

  responseFuture.onComplete {
    case Success(response) =>
      // VERY IMPORTANT
      response.discardEntityBytes()
      println(s"The request was successful and returned: $response")
    case Failure(exception) =>
      println(s"The reqyest failed with $exception")
  }

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniels-account"),
    CreditCard("1234-1234-4321-4321", "321", "my-awesome-account")
  )

  val paymentRequest = creditCards.map(creditCard => PaymentRequest(creditCard, "rtjvm-store-account", 99))
  val serverHttpRequests = paymentRequest.map(paymentRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("http://localhost:8080/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentRequest.toJson.prettyPrint
      )
    )
  )

  Source(serverHttpRequests)
    .mapAsync(10)(request => Http().singleRequest(request))
    .runForeach(println)


}
