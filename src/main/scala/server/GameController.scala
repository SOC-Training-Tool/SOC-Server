package server

import io.grpc.stub.StreamObserver
import protos.soc.game.SubscribeRequest.SubscriptionType
import protos.soc.game.{CreateGameResponse, GameMessage}
import soc.behaviors.MoveResultProvider
import soc.board.{BaseBoardConfiguration, BoardGenerator}
import soc.core.GameRules
import soc.inventory.{Inventory, InventoryHelperFactory}
import soc.inventory.Inventory.PerfectInfo
import soc.moves.CatanMove
import soc.playerRepository.PlayerContext
import soc.storage.GameId

import scala.collection.mutable.HashMap
import scala.concurrent.ExecutionContext
import scala.util.Random

class GameController[GAME <: Inventory[GAME], BOARD <: BaseBoardConfiguration](implicit
  ec: ExecutionContext,
  gameInventoryHelperFactory: InventoryHelperFactory[GAME],
  boardGenerator: BoardGenerator[BOARD],
  random: Random
) {

  private val games: HashMap[String, GameContext[GAME, BOARD]] = HashMap.empty
  private val builders: HashMap[String, GameBuilder[GAME, BOARD]] = HashMap.empty

  def getGameContext(gameId: String): Option[GameContext[GAME, BOARD]] = games.get(gameId)
  def getPlayerContext(gameId: String, position: Int): Option[PlayerContext[GAME]] = getGameContext(gameId).flatMap(_.getPlayer(position))

  def createGame(
    gameId: GameId,
    boardConfig: BOARD,
    gameRules: GameRules,
    resultProvider: MoveResultProvider[GAME]
  ): Unit = {
    println("Create Game" + gameId)
    val gameContextBuilder = GameContext.builder[GAME, BOARD](gameId, boardConfig, resultProvider, None, gameRules)
    builders.put(gameId.key, gameContextBuilder)
  }

  def startGame(gameId: String): Boolean = if (builders.get(gameId).fold(false)(_.canStart)) {
    println("Starting game " + gameId)
    val gameContext = builders.remove(gameId).get
    games.put(gameId, gameContext.start)
    true
  } else false

  def subscribePlayer(gameId: String, playerName: String, subscriptionType: SubscriptionType, position: Option[Int], responseObserver: StreamObserver[GameMessage]) =  if (builders.get(gameId).fold(false)(_.canSubscribe(subscriptionType, position))) {
    println("Registering listener " + playerName)
    val gameBuilder = builders.remove(gameId).get
    builders.put(gameId, gameBuilder.subscribePlayer(playerName, position, responseObserver))
    true
  } else false

}

object GameController {

  def apply[GAME <: Inventory[GAME], BOARD <: BaseBoardConfiguration](implicit
    ec: ExecutionContext,
    gameInventoryHelperFactory: InventoryHelperFactory[GAME],
    boardGenerator: BoardGenerator[BOARD],
    random: Random
  ): GameController[GAME, BOARD] = new GameController()



}
