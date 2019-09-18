package server

import java.util.logging.Logger

import akka.actor.typed.ActorSystem
import io.grpc.{Server, ServerBuilder}
import io.grpc.stub.StreamObserver
import soc.akka.{MoveResultProvider, RandomMoveResultProvider}
import soc.game.{GameRules, RollDiceMove}
import soc.game.board.{BaseBoardConfiguration, BaseCatanBoard}
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo}
import soc.playerRepository.{PlayerContext, PlayerRepository}
import soc.protos.game.{CatanServerGrpc, CreateGameRequest, CreateGameResponse, GameUpdate, MoveRequest, MoveResponse, StartGameRequest, StartGameResponse, SubscribeRequest}
import soc.storage.{GameId, SimulatedGame}

import scala.collection.mutable.HashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Random

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

  implicit val random = new Random
  implicit val playerRepo = new PlayerRepository

  import soc.game.inventory.InventoryHelper._
  import soc.game.board.BaseCatanBoard._

  private val games: HashMap[String, GameContext[_, _, _]] = HashMap.empty
  private val builders: HashMap[String, GameBuilder[_, _, _]] = HashMap.empty
  private var counter = 0

  override def createGame(req: CreateGameRequest) = {
    counter += 1
    val gameId = GameId(SimulatedGame, "", counter); // TODO: Create real ids (probably don't want them to be integers)

    // Most of this information should come from CreateGameRequest
    val boardConfig = BaseCatanBoard.randomBoard
    val gameRules = GameRules.default

    val moveProvider = RandomMoveResultProvider(gameRules)
    val randomMoveResultProvider = ActorSystem(MoveResultProvider.moveResultProvider(moveProvider), "resultProvider")


    val gameContextBuilder = GameContext.builder[PerfectInfo, NoInfo, BaseBoardConfiguration](gameId, boardConfig, randomMoveResultProvider, None, gameRules)
    builders.put(gameId.key, gameContextBuilder)
    println("Create game with id " + gameId)
    val reply = CreateGameResponse(gameId = gameId.key)
    Future.successful(reply)
  }

  override def startGame(req: StartGameRequest) = {
    val reply = StartGameResponse()
    builders.get(req.gameId) match {
      case Some(gameContext) => {
        println("Starting game " + req.gameId)
        games.put(req.gameId, gameContext.start)
        builders.remove(req.gameId)
      }
      case None => println("No game found with " + req.gameId)
    }
    Future.successful(reply)
  }

  override def subscribe(req: SubscribeRequest, responseObserver: StreamObserver[GameUpdate]) = {
    builders.get(req.gameId) match {
      case Some(gameContext) => {
        println("Registering listener " + req.name)
        if (!playerRepo.contains(req.name)) playerRepo.addPlayer(new PlayerContext[PerfectInfo, NoInfo](req.name))

        //Todo throw error if player already declared position
        builders.put(req.gameId, gameContext.subscribePlayer(req.name, None, responseObserver))
      }
      case None => println("No game found with " + req.gameId + " cannot register listener: " + req.name)
    }
  }

  override def move(req: MoveRequest) = {
    val playerContext = games.get(req.gameId).flatMap(_.getPlayer(req.position)).getOrElse(throw new Exception(""))
    playerContext.receiveMove(req.gameId, req.position, RollDiceMove)
    Future.successful(new MoveResponse("ERROR: Unimplemented"))
  }

  // TODO: Add methods for registerPlayer, setBoard, setState
  // ie. The pattern should be to create a game, then configure the game how you want, then start.
  // Note: We can create defaults so not every step has to be called every time
  // Further, we can create parameterization on the CreateGameRequest, start=Boolean.
  // This could allow a client to create and start a game with one call.

}