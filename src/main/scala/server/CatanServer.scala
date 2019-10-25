package server

import java.util.logging.Logger

import io.grpc.{Server, ServerBuilder}
import io.grpc.stub.StreamObserver
import soc.behaviors.RandomMoveResultProvider
import soc.board.{BaseBoardConfiguration, BaseCatanBoard}
import soc.core.GameRules
import soc.inventory.Inventory._
import soc.moves.CatanMove
import protos.soc.game.{CatanServerGrpc, CreateGameRequest, CreateGameResponse, GameMessage, GetMovesRequest, GetMovesResponse, MoveResponse, StartGameRequest, StartGameResponse, SubscribeRequest, TakeActionRequest}
import soc.storage.{GameId, SimulatedGame}
import protocoder.ProtoCoder.ops._
import protocoder.implicits.MoveProto._

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

  import soc.inventory.InventoryHelper._
  import soc.board.BaseCatanBoard._

  val gameController: GameController[PerfectInfo, BaseBoardConfiguration] = GameController[PerfectInfo, BaseBoardConfiguration]

  //private val games: HashMap[String, GameContext[PerfectInfo, BaseBoardConfiguration]] = HashMap.empty
  //private val builders: HashMap[String, GameBuilder[PerfectInfo, BaseBoardConfiguration]] = HashMap.empty
  private var counter = 0

  override def createGame(req: CreateGameRequest): Future[CreateGameResponse] = this.synchronized  {
    val gameId = GameId(SimulatedGame, "", counter); // TODO: Create real ids (probably don't want them to be integers)
    counter += 1

    // Most of this information should come from CreateGameRequest
    val boardConfig = BaseCatanBoard.randomBoard
    val gameRules = GameRules.default
    val moveProvider = RandomMoveResultProvider(gameRules)

    gameController.createGame(gameId, boardConfig, gameRules, moveProvider)
    println("Create game with id " + gameId)
    val reply = CreateGameResponse(gameId = gameId.key)
    Future.successful(reply)
  }

  override def startGame(req: StartGameRequest): Future[StartGameResponse] = this.synchronized  {
    val reply = StartGameResponse()
    gameController.startGame(req.gameId)
    Future.successful(reply)
  }

  override def subscribe(req: SubscribeRequest, responseObserver: StreamObserver[GameMessage]) = this.synchronized {
    gameController.subscribePlayer(req.gameId, req.name, req.`type`, req.position, responseObserver)
  }

  override def takeAction(req: TakeActionRequest) = this.synchronized  {
    val action: CatanMove = req.action.proto
    gameController
      .getPlayerContext(req.gameId, req.position)
      .map(_.receiveMove(action))
      .getOrElse(Future.successful(MoveResponse(s"Could not find player ${req.position}")))
  }

  override def getAllMoves(request: GetMovesRequest): Future[GetMovesResponse] = this.synchronized {

    import protocoder.ProtoCoder.ops._
    import protocoder.implicits.StateProto._
    import protocoder.implicits.ResourceProto._
    import protocoder.implicits.MoveProto._

    implicit val gameRules = GameRules.default
    Future.successful(
      GetMovesResponse(
        soc.moves.CatanPossibleMoves(request.gameState.proto, request.privateInventory.proto, request.position).getPossibleMovesForState.map(_.proto).toSeq
      )
    )
  }
}