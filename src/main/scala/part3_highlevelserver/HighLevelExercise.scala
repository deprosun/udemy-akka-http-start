package part3_highlevelserver

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.Http
import akka.util.Timeout
import spray.json._

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.{Failure, Success}

case class Person(pin: Int, name: String)

trait PersonRegistryJsonProtocol extends DefaultJsonProtocol {
  implicit val guitarFormat = jsonFormat2(Person)
}

object HighLevelExercise extends App with PersonRegistryJsonProtocol {
  implicit val system = ActorSystem("HighLevelExercise")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  /**
   * Exercise:
   * - GET /api/people: retrieve ALL the people you have registered
   * - GET /api/people/pin: retrieve the person with that PIN, return as JSON
   * - GET /api/people?pin=X (same)
   * - POST /api/people with a JSON payload denoting a person, add that person to your database
   *  - extract the HTTP request's payload (entity)
   *    - extract the request
   *    - process the entity's data
   */


  var people = List(
    Person(1, "Alice"),
    Person(2, "Bob"),
    Person(3, "Charlie")
  )

  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  implicit val defaultTimeout: Timeout = Timeout(3 seconds)

  val route = pathPrefix("api" / "people") {
    get {
      (path(IntNumber) | parameter('pin.as[Int])) { pin =>
        complete(
          Future(people.filter(_.pin == pin))
            .map(_.toJson.prettyPrint)
            .map(toHttpEntity)
        )
      } ~ pathEndOrSingleSlash {
        complete(
          Future(people)
            .map(_.toJson.prettyPrint)
            .map(toHttpEntity)
        )
      }
    } ~ (post & extractRequest & extractLog) {
      (httpRequest, log) =>
        val strictEntityFuture = httpRequest.entity.toStrict(3 seconds)
        val personFuture = strictEntityFuture.map(_.data.utf8String.parseJson.convertTo[Person])

        onComplete(personFuture) {
          case Success(person) =>
            log.info(s"Got person: $person")
            people = people :+ person
            complete(StatusCodes.OK)
          case Failure(ex) => failWith(ex)
        }
      // side effect
      //        personFuture.onComplete {
      //          case Success(person) =>
      //            log.info(s"Got person: $person")
      //            people = people :+ person
      //          case Failure(exception) =>
      //            log.warning(s"Something failed with fetching the person from the entity: $exception")
      //        }
      //
      //        complete(personFuture.map(_ => StatusCodes.OK).recover {
      //          case _ => StatusCodes.InternalServerError
      //        })

    }
  }

  Http().bindAndHandle(route, "localhost", 8080)
}
