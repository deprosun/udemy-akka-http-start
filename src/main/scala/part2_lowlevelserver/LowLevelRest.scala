package part2_lowlevelserver

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.Uri.Query
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import part2_lowlevelserver.GuitarDB.{AddGuitar, CreateGuitar, FindAllGuitars, FindGuitar, FindGuitarsInStock, FindGuitarOutOfStock, GuitarCreated}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

// step 1
import spray.json._

case class Guitar(make: String, model: String, quantity: Int)

object GuitarDB {
  case class CreateGuitar(guitar: Guitar)

  case class GuitarCreated(id: Int)

  case class FindGuitar(id: Int)

  case class AddGuitar(id: Int, quantity: Int)

  case object FindAllGuitars

  case class FindGuitarsInStock(inStock: Boolean)

  case object FindGuitarOutOfStock
}

class GuitarDB extends Actor with ActorLogging {

  import GuitarDB._

  var guitars: Map[Int, Guitar] = Map()
  var currentGuitarId: Int = 0

  override def receive: Receive = {
    case FindAllGuitars =>
      log.info("Searching for all guitars")
      sender() ! guitars.values.toList
    case FindGuitar(id: Int) =>
      log.info(s"Searching guitar by $id")
      sender() ! guitars.get(id)
    case FindGuitarsInStock(inStock) =>
      log.info(s"Searching for all guitars ${if(inStock) "in" else "out of"} stock")
      if (inStock)
        sender() ! guitars.values.filter(_.quantity > 0)
      else
        sender() ! guitars.values.filter(_.quantity == 0)
    case FindGuitarOutOfStock =>
      log.info(s"Searching guitar that are out of stock")
      sender() ! guitars.values.filter(_.quantity <= 0)
    case AddGuitar(id: Int, q: Int) =>
      guitars = guitars + (id -> guitars(id).copy(quantity = guitars(id).quantity + q))
      sender() ! guitars.get(id)
    case CreateGuitar(guitar) =>
      log.info(s"Adding guiter $guitar with id $currentGuitarId")
      guitars = guitars + (currentGuitarId -> guitar)
      sender() ! GuitarCreated(currentGuitarId)
      currentGuitarId += 1
  }

}

// step 2
trait GuitarStoreJsonProtocol extends DefaultJsonProtocol {
  // step 3
  implicit val guitarFormat = jsonFormat3(Guitar)
}

object LowLevelRest extends App with GuitarStoreJsonProtocol {

  implicit val system = ActorSystem("LowLevelRest")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  /*
    - GET on localhost:8080/api/guitar => ALL the guitars in the store
    - GET on localhost:8080/api/guitar?id=X => The guitar associated with id X
    - POST on localhost:8080/api/guitar => insert the guitar into the store
   */

  // JSON --> marshalling:
  // marshalling is the process of serializing our data
  // to a wire format that our http client can understand.
  val simpleGuitar = Guitar("Fender", "Stratocaster", 1)
  println(simpleGuitar.toJson.prettyPrint)

  // unmarshalling
  val simpleGuitaJsonString =
    """
      |{
      |  "make": "Fender",
      |  "model": "Stratocaster",
      |  "quantity" : 1
      |}
      |""".stripMargin

  println(simpleGuitaJsonString.parseJson.convertTo[Guitar])

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

  def getGuitar(query: Query): Future[HttpResponse] = {
    val guitarId = query.get("id").map(_.toInt) // Option[Int]
    guitarId match {
      case None => Future(HttpResponse(StatusCodes.NotFound))
      case Some(id) =>
        val guitarFuture = (guitarDb ? FindGuitar(id)).mapTo[Option[Guitar]]
        guitarFuture.map {
          case None => HttpResponse(StatusCodes.NotFound)
          case Some(guitar) => HttpResponse(
            entity = HttpEntity(
              ContentTypes.`application/json`,
              guitar.toJson.prettyPrint
            )
          )
        }
    }
  }

  /*
    server code
    we're gonna use async handler because we need to communicate
    with an actor. whenever, we need to communicate with an external
    server, use Futures.
   */
  implicit val defaultTimeout: Timeout = Timeout(3 seconds)

  def getGuitarsInStock(query: Query): Future[HttpResponse] = {
    val isInStock = query.get("inStock").map(_.toLowerCase).contains("true")
    val completed = if (isInStock)
      (guitarDb ? FindGuitarsInStock(isInStock)).mapTo[List[Guitar]]
    else
      (guitarDb ? FindGuitarOutOfStock).mapTo[List[Guitar]]

    completed.map {
      case x if x.isEmpty =>
        HttpResponse(StatusCodes.NotFound)
      case x => HttpResponse(
        entity = HttpEntity(
          ContentTypes.`application/json`,
          x.toJson.prettyPrint
        )
      )
    }

  }

  def addToStock(query: Query): Future[HttpResponse] = {
    //?id=X&quantity=Y which adds Y guitars to the stock for the guitar with id X
    val id = query.get("id").map(_.toInt)
    val quantity = query.get("quantity").map(_.toInt)
    val completed = (guitarDb ? AddGuitar(id.get, quantity.get)).mapTo[Option[Guitar]]

    completed.map {
      case None => HttpResponse(StatusCodes.NotFound)
      case x => HttpResponse(
        entity = HttpEntity(
          ContentTypes.`application/json`,
          x.toJson.prettyPrint
        )
      )
    }
  }

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(post, uri@Uri.Path("/api/guitar/inventory"), _, _, _) =>
      val query = uri.query()
      if (post == HttpMethods.GET) {
        getGuitarsInStock(query)
      } else {
        addToStock(query)
      }
    case HttpRequest(HttpMethods.GET, uri@Uri.Path("/api/guitar"), _, _, _) =>
      /*
        query parameter code here
       */
      val query = uri.query()
      if (query.isEmpty) {
        val guitarFuture = (guitarDb ? FindAllGuitars).mapTo[List[Guitar]]
        guitarFuture.map { guitars =>
          HttpResponse(
            entity = HttpEntity(
              ContentTypes.`application/json`,
              guitars.toJson.prettyPrint
            )
          )
        }
      } else {
        // fetch guiter associated to the guitar id
        // localhost:8080/api/guitar?id=45
        getGuitar(query)
      }

    case HttpRequest(HttpMethods.POST, Uri.Path("/api/guitar"), _, entity, _) =>
      //entities are a Source[ByteString]
      val strictEntityFuture = entity.toStrict(3 seconds)
      strictEntityFuture.flatMap { strictEntity =>
        val guitarJsonString = strictEntity.data.utf8String
        val guitar = guitarJsonString.parseJson.convertTo[Guitar]
        val guitarCreatedFuture = (guitarDb ? CreateGuitar(guitar)).mapTo[GuitarCreated]
        guitarCreatedFuture.map { _ =>
          HttpResponse(StatusCodes.OK)
        }

      }
    // this is really important
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future {
        HttpResponse(status = StatusCodes.NotFound)
      }
  }

  Http().bindAndHandleAsync(requestHandler, "localhost", 8080)

  /**
   * Exercise: enhance the Guitar case class with a quantity field, by default 0
   * - GET to /api/guitar/inventory?inStock=true/false which returns the guitars in stock as a JSON
   * - POST to /api/guitar/inventory?id=X&quantity=Y which adds Y guitars to the stock for the guitar with id X
   *
   */
}
