package part3_highlevelserver

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout

import scala.concurrent.duration._
import part2_lowlevelserver.{Guitar, GuitarDB, GuitarStoreJsonProtocol}

import scala.concurrent.Future
import scala.language.postfixOps

// step 1
import spray.json._

object HighLevelExample extends App with GuitarStoreJsonProtocol {
  implicit val system = ActorSystem("HighLevelExample")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  import GuitarDB._

  /*
    GET /api/guitar fetches ALL the guitars in the store
    GET /api/guitar?id=X fetches the guitar with id X
    GET /api/guitar/X fetches the guitar with id X
    GET /api/guitar/inventory?inStock=true
   */

  /*
  setup
 */
  val guitarDb = system.actorOf(Props[GuitarDB], "LowlLevelGuitarDb")
  val guitarList = List(
    Guitar("Fender", "Stratocaster", 1),
    Guitar("Gibson", "Les Paul", 1),
    Guitar("Martin", "LX1", 1)
  )

  guitarList.foreach { guitar =>
    guitarDb ! CreateGuitar(guitar)
  }

  implicit val timeout = Timeout(2 seconds)


  val guitarServerRoute =
    path("api" / "guitar") {
      parameter('id.as[Int]) { guitarId =>
        get {
          val guitarFuture: Future[Option[Guitar]] = (guitarDb ? FindGuitar(guitarId)).mapTo[Option[Guitar]]
          val entityFuture = guitarFuture.map { guitarOption =>
            HttpEntity(
              ContentTypes.`application/json`,
              guitarOption.toJson.prettyPrint
            )
          }
          complete(entityFuture)
        }
      } ~ get {
        val guitarsFuture = (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
        val entityFuture = guitarsFuture.map {
          guitars =>
            HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
        }
        complete(entityFuture)
      }
    } ~ path("api" / "guitar" / IntNumber) { guitarId =>
      get {
        val guitarFuture: Future[Option[Guitar]] = (guitarDb ? FindGuitar(guitarId)).mapTo[Option[Guitar]]
        val entityFuture = guitarFuture.map { guitarOption =>
          HttpEntity(
            ContentTypes.`application/json`,
            guitarOption.toJson.prettyPrint
          )
        }
        complete(entityFuture)
      }
    } ~ path("api" / "guitar" / "inventory") {
      get {
        parameter('inStock.as[Boolean]) { inStock =>
          val guitarFuture: Future[List[Guitar]] = (guitarDb ? FindGuitarsInStock(inStock)).mapTo[List[Guitar]]
          val entityFuture = guitarFuture.map { guitars =>
            HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
          }
          complete(entityFuture)
        }
      }
    }

  def toHttpEntity(payload: String) = HttpEntity(ContentTypes.`application/json`, payload)

  val simplifiedGuitarServerRoute = (pathPrefix("api" / "guitar") & get) {
    path("inventory") {
      parameter('inStock.as[Boolean]) { inStock =>
        complete((guitarDb ? FindGuitarsInStock(inStock))
          .mapTo[List[Guitar]]
          .map(_.toJson.prettyPrint)
          .map(toHttpEntity))
      }
    } ~ (path(IntNumber) | parameter('id.as[Int])) { guitarId =>
      complete((guitarDb ? FindGuitar(guitarId))
        .mapTo[Option[Guitar]]
        .map(_.toJson.prettyPrint)
        .map(toHttpEntity))
    } ~ pathEndOrSingleSlash {
      complete((guitarDb ? FindAllGuitars)
        .mapTo[List[Guitar]]
        .map(_.toJson.prettyPrint)
        .map(toHttpEntity))
    }
  }
  Http().bindAndHandle(simplifiedGuitarServerRoute, "localhost", 8080)

}
