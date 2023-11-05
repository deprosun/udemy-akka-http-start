package part3_highlevelserver

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import part3_highlevelserver.GameAreaMap.{AddPlayer, GetAllPlayers, GetPlayer, GetPlayerByClass, OperationSuccess, RemovePlayer}

import scala.util.{Failure, Success}


case class Player(nickname: String, characterClass: String, level: Int)

object GameAreaMap {
  case object GetAllPlayers

  case class GetPlayer(nickname: String)

  case class GetPlayerByClass(characterClass: String)

  case class AddPlayer(player: Player)

  case class RemovePlayer(player: Player)

  case object OperationSuccess
}

class GameAreaMap extends Actor with ActorLogging {
  var players = Map[String, Player]()

  override def receive: Receive = {
    case GetAllPlayers =>
      log.info("Getting all players")
      sender() ! players.values.toList

    case GetPlayer(nickname) =>
      log.info(s"Getting player with nickname $nickname")
      sender() ! players.get(nickname)

    case GetPlayerByClass(characterClass) =>
      log.info(s"Getting all players with the character class $characterClass")
      sender() ! players.values.toList.filter(_.characterClass == characterClass)

    case AddPlayer(player) =>
      log.info(s"Trying to add player $player")
      players = players + (player.nickname -> player)
      sender() ! OperationSuccess

    case RemovePlayer(player) =>
      log.info(s"Trying to remove player $player")
      players = players - (player.nickname)
      sender() ! OperationSuccess
  }
}

import spray.json._

trait PlayerJsonProtocol extends DefaultJsonProtocol {
  implicit val playerFormat = jsonFormat3(Player)
}

object MarshallingJSON extends App with PlayerJsonProtocol with SprayJsonSupport {
  implicit val system = ActorSystem("MarshallingJSON")
  implicit val materalizer = ActorMaterializer()

  import system.dispatcher

  val rtjvmGameMap = system.actorOf(Props[GameAreaMap], "rockTheJVMGameAreaMap")
  val playersList = List(
    Player("martin_killz_u", "Warrior", 70),
    Player("rolandbraveheart", "Elf", 67),
    Player("daniel_rock03", "Wizard", 30)
  )

  playersList.foreach { player =>
    rtjvmGameMap ! AddPlayer(player)
  }

  /*
    - GET /api/player, return all the players in the map, as JSON
    - GET /api/player/(nickname), return the player with the given nickname (as JSON)
    - GET /api/player?nickname=X, does the same
    - GET /api/player/class/(chatCLass), returns all the players with given character class
    - POST /api/player with JSON payload, adds the players to the map
    - (Exercise) DELETE /api/player with JSON payload, removes the player from the map
   */

  implicit val timeout = Timeout(2 seconds)


  val rtjvmGameRouteSkel =
    pathPrefix("api" / "player") {
      get {
        path("class" / Segment) { characterClass =>
          //todo 1: get all the players with characterClass
          val playersFuture = (rtjvmGameMap ? GetPlayerByClass(characterClass)).mapTo[List[Player]]
          val entityFuture = playersFuture.map { players =>
            HttpEntity(
              ContentTypes.`application/json`,
              players.toJson.prettyPrint
            )
          }
          complete(entityFuture)
        } ~ (path(Segment) | parameter('nickname)) { nickname =>
          val playersFuture = (rtjvmGameMap ? GetPlayer(nickname)).mapTo[Option[Player]]
          val entityFuture = playersFuture.map { player =>
            HttpEntity(
              ContentTypes.`application/json`,
              player.toJson.prettyPrint
            )
          }
          complete(entityFuture)
        } ~ pathEndOrSingleSlash {
          val playersFuture = (rtjvmGameMap ? GetAllPlayers).mapTo[List[Player]]
          val entityFuture = playersFuture.map { players =>
            HttpEntity(
              ContentTypes.`application/json`,
              players.toJson.prettyPrint
            )
          }
          complete(entityFuture)
        }
      } ~ post {
        entity(as[Player]) { player =>
          val playerFuture = (rtjvmGameMap ? AddPlayer(player)).map(_ => StatusCodes.OK)
          complete(playerFuture)
        }
      } ~ delete {
        entity(as[Player]) { player =>
          val playerFuture = (rtjvmGameMap ? RemovePlayer(player)).map(_ => StatusCodes.OK)
          complete(playerFuture)
        }
      }
    }

  Http().bindAndHandle(rtjvmGameRouteSkel, "localhost", 8080)




}
