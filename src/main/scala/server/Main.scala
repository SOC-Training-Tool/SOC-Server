package server

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import soc.protos.game.{CatanServerGrpc, CreateGameRequest, CreateGameResponse, StartGameRequest, StartGameResponse}

import scala.concurrent.{ExecutionContext, Future}

import scala.collection.mutable.HashMap

import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.messages.StateMessage
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo}

import soc.akka.{GameBehavior, PlayerBehavior, MoveSaverBehavior, MoveResultProvider, RandomMoveResultProvider}
import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.messages.{GameMessage, Terminate}
import soc.game.dice.NormalDice
import soc.game.player.moveSelector.PossibleMoveSelector
import soc.game._
import soc.game.board.{BaseBoardConfiguration, BaseCatanBoard}
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo}
import soc.game.inventory._
import soc.storage.aws.AWSMoveSaver

import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import soc.aws.client.CatanGameStoreClientFactory
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory.InventoryManager._
import soc.simulation.SimulationQueue

import soc.game.{GameConfiguration, GameRules}

object GameContext {
  implicit val random = new Random()

  val boardConfig = BaseCatanBoard.randomBoard
  import BaseCatanBoard._

  val dice = NormalDice()

 
  implicit val gameRules = GameRules()

  val dCardDeck: List[DevelopmentCard] = DevelopmentCardDeckBuilder.buildDeckByCardTypeAndAmount(
    Map(Knight -> Knight.initAmount,
      CatanPoint -> CatanPoint.initAmount,
      RoadBuilder -> RoadBuilder.initAmount,
      Monopoly -> Monopoly.initAmount,
      YearOfPlenty -> YearOfPlenty.initAmount)
  )

  import CatanResourceSet._
  import io.circe.generic.auto._
  val awsGameSaver = new AWSMoveSaver[BaseBoardConfiguration](CatanGameStoreClientFactory.createClient())
  val moveSaverActor: ActorRef[GameMessage] = ActorSystem(MoveSaverBehavior.moveSaverBehavior(awsGameSaver), "moveSaver")

  val randSelector = PossibleMoveSelector.randSelector[NoInfo]

  val players = Map(
    ("randomPlayer", 0) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player0"),
    ("randomPlayer", 1) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player1"),
    ("randomPlayer", 2) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player2"),
    ("randomPlayer", 3) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player3")
  )

  val moveProvider = new RandomMoveResultProvider(dice, dCardDeck)
  val randomMoveResultProvider = ActorSystem(MoveResultProvider.moveResultProvider(moveProvider), "resultProvider")
  val config = GameConfiguration[PerfectInfo, NoInfo, BaseBoardConfiguration](1, boardConfig, players, randomMoveResultProvider, None, gameRules)
}

class GameContext(val gameId: String) {
  private var game: ActorSystem[StateMessage[PerfectInfo, NoInfo]] = null
  import GameContext._

  def start(): Unit = {
    game = ActorSystem(GameBehavior.gameBehavior(config), s"SettlersOfCatan${gameId}")
  }

}


object CatanServer{
  private val logger = Logger.getLogger(classOf[CatanServer].getName)
  import GameContext._

  //val game = ActorSystem(GameBehavior.gameBehavior(config), s"SettlersOfCatan${1}")

  def main(args: Array[String]): Unit = {
    val server = new CatanServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class CatanServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    server = ServerBuilder.forPort(CatanServer.port).addService(CatanServerGrpc.bindService(new CatanServerImpl, executionContext)).build.start
    CatanServer.logger.info("Server started, listening on " + CatanServer.port)
    sys.addShutdownHook {
      System.err.println("*** shutting down gRPC server since JVM is shutting down")
      self.stop()
      System.err.println("*** server shut down")
    }
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

}

class CatanServerImpl extends CatanServerGrpc.CatanServer {

  private val games: HashMap[String, GameContext] = HashMap()
  
  override def createGame(req: CreateGameRequest) = {
    val gameId = "game1"
    val gameContext = new GameContext(gameId)
    games.put(gameId, gameContext)
    val reply = CreateGameResponse(gameId = "foo")
    Future.successful(reply)
  }

  override def startGame(req: StartGameRequest) = {
    val reply = StartGameResponse()
    for (i <- games.get("game1") ) { i.start() }
    Future.successful(reply)
  }

}