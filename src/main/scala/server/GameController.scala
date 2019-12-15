package server

import java.util.logging.Logger

import io.grpc.stub.StreamObserver
import protos.soc.game.SubscribeRequest.SubscriptionType
import protos.soc.game.{GameMessage, GameResults}
import soc.aws.client.CatanGameStoreClient
import soc.behaviors.MoveResultProvider
import soc.board.{BaseBoardConfiguration, BoardConfiguration, BoardGenerator}
import soc.core.GameRules
import soc.inventory.{Inventory, InventoryHelperFactory}
import soc.playerRepository.PlayerContext
import soc.storage.GameId

import scala.collection.mutable
import scala.collection.mutable.HashMap
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class GameController[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](gameStoreClient: CatanGameStoreClient, logger: Logger)(implicit
  ec: ExecutionContext,
  gameInventoryHelperFactory: InventoryHelperFactory[GAME],
  boardGenerator: BoardGenerator[BOARD],
  random: Random
) {

  val MAX_IDS = 1000000
  private val recentlyCompletedIds = mutable.Queue.empty[String]
  private val gamesInProgress: HashMap[String, GameContext[GAME, BOARD]] = HashMap.empty

  def getGameContext(gameId: String): Future[Option[GameContext[GAME, BOARD]]] = Future.successful(gamesInProgress.get(gameId))

  def getPlayerContext(gameId: String, position: Int): Future[Option[PlayerContext[GAME]]] = getGameContext(gameId).map(_.flatMap(_.getPlayer(position)) )

  def createGame(
    gameId: GameId,
    boardConfig: BOARD,
    gameRules: GameRules,
    resultProvider: MoveResultProvider[GAME],
    saveGame: Boolean
  ): Unit = {
    println("Create Game" + gameId)
    val gameContextBuilder = GameContext[GAME, BOARD](this, gameId, boardConfig, resultProvider, 4, gameRules, saveGame, logger)
    gamesInProgress.put(gameId.key, gameContextBuilder)
  }

  def startGame(gameId: String): Boolean = gamesInProgress.get(gameId).fold(false) { game =>
    println("Starting game " + gameId)
    game.start
    true
  }

  def subscribe(gameId: String, id: String, subscriptionType: SubscriptionType, position: Option[Int], responseObserver: StreamObserver[GameMessage]) =  gamesInProgress.get(gameId).fold(false){ game =>
    if (game.canSubscribe(subscriptionType, position)) {
      println("Registering listener " + id)
      game.subscribe(subscriptionType, id, position, responseObserver)
      true
    } else false
  }

  def saveGame(game: GameResults): Future[Unit] = this.synchronized {
    Future{
      gameStoreClient.save(game.gameId, game.results.toByteArray, game.initialBoard.toByteArray)
    }.map { _ =>
      gamesInProgress.remove(game.gameId)
      recentlyCompletedIds.enqueue(game.gameId)
      if (recentlyCompletedIds.size > MAX_IDS) recentlyCompletedIds.dequeue()
    }

  }
}

object GameController {

  def apply[GAME <: Inventory[GAME], BOARD <: BaseBoardConfiguration](gameStoreClient: CatanGameStoreClient, logger: Logger)(implicit
    ec: ExecutionContext,
    gameInventoryHelperFactory: InventoryHelperFactory[GAME],
    boardGenerator: BoardGenerator[BOARD],
    random: Random
  ): GameController[GAME, BOARD] = new GameController(gameStoreClient, logger)



}
