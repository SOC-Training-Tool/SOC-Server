package server

// TODO: Clean up imports
import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import io.grpc.stub.StreamObserver
import soc.protos.game.{CatanServerGrpc, CreateGameRequest, CreateGameResponse, StartGameRequest, StartGameResponse, MoveRequest, MoveResponse, SubscribeRequest, GameUpdate}

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
import scala.util.{Success, Failure}

object GameContext {
  implicit val random = new Random()

  val boardConfig = BaseCatanBoard.randomBoard

  val dice = NormalDice()

 
  implicit val gameRules = GameRules()

  val dCardDeck: List[DevelopmentCard] = DevelopmentCardDeckBuilder.buildDeckByCardTypeAndAmount(
    Map(Knight -> Knight.initAmount,
      CatanPoint -> CatanPoint.initAmount,
      RoadBuilder -> RoadBuilder.initAmount,
      Monopoly -> Monopoly.initAmount,
      YearOfPlenty -> YearOfPlenty.initAmount)
  )

  val randSelector = PossibleMoveSelector.randSelector[NoInfo]
  val players = Map(
    ("randomPlayer", 0) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector, "1"), "player0"),
    ("randomPlayer", 1) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector, "2"), "player1"),
    ("randomPlayer", 2) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector, "3"), "player2"),
    ("randomPlayer", 3) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector, "4"), "player3")
  )


  import CatanResourceSet._
  import io.circe.generic.auto._
  //val awsGameSaver = new AWSMoveSaver[BaseBoardConfiguration](CatanGameStoreClientFactory.createClient())
  //val moveSaverActor: ActorRef[GameMessage] = ActorSystem(MoveSaverBehavior.moveSaverBehavior(awsGameSaver), "moveSaver")


  val moveProvider = new RandomMoveResultProvider(dice, dCardDeck)
  val randomMoveResultProvider = ActorSystem(MoveResultProvider.moveResultProvider(moveProvider), "resultProvider")
}

class GameContext(val gameId: String) {
  private var game: ActorSystem[StateMessage[PerfectInfo, NoInfo]] = null
  private val subscribers: HashMap[String, StreamObserver[GameUpdate]] = new HashMap()
  import GameContext._
  import BaseCatanBoard._

  def start(): Unit = {
    val config = GameConfiguration[PerfectInfo, NoInfo, BaseBoardConfiguration](gameId.toInt, boardConfig, players, randomMoveResultProvider, None, gameRules)
    // TODO: Hold onto a reference to this game / future 
    // This will allow us to query a game status (ie, what is the current turn), save it, terminate it, etc.
    val future = ActorSystem(GameBehavior.gameBehavior(config, subscribers), s"SettlersOfCatan${gameId}").whenTerminated
    // TODO: Clean up the game from the hashtable of currently running games
    //future onComplete {
    //  case Success() => 
    //  case Failure(t) => println("An error has occurred: " + t.getMessage)
    //}
  }

  def subscribe(name: String, observer: StreamObserver[GameUpdate]): Unit = {
    subscribers += (name -> observer)
  }
}


object CatanServer {
  private val logger = Logger.getLogger(classOf[CatanServer].getName)

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

private class CatanServerImpl extends CatanServerGrpc.CatanServer {

  private val games: HashMap[String, GameContext] = HashMap()
  private var counter = 0
  
  override def createGame(req: CreateGameRequest) = {
    counter += 1
    val gameId = counter.toString; // TODO: Create real ids (probably don't want them to be integers)
    val gameContext = new GameContext(gameId)
    games.put(gameId, gameContext)
    println("Create game with id " + gameId)
    val reply = CreateGameResponse(gameId = gameId)
    Future.successful(reply)
  }

  override def startGame(req: StartGameRequest) = {
    val reply = StartGameResponse()
    games.get(req.gameId) match {
      case Some(gameContext) => {
        println("Starting game " + req.gameId)
        gameContext.start()
      }
      case None => println("No game found with " + req.gameId)
    }
    Future.successful(reply)
  }

  override def subscribe(req: SubscribeRequest, responseObserver: StreamObserver[GameUpdate]) = {
    games.get(req.gameId) match {
      case Some(gameContext) => {
        println("Registering listener " + req.name)
        gameContext.subscribe(req.name, responseObserver)
      }
      case None => println("No game found with " + req.gameId + " cannot register listener: " + req.name)
    }
  }

  override def move(req: MoveRequest) = {
    Future.successful(new MoveResponse("ERROR: Unimplemented"))
  }

  // TODO: Add methods for registerPlayer, setBoard, setState
  // ie. The pattern should be to create a game, then configure the game how you want, then start.
  // Note: We can create defaults so not every step has to be called every time
  // Further, we can create parameterization on the CreateGameRequest, start=Boolean.
  // This could allow a client to create and start a game with one call.

}