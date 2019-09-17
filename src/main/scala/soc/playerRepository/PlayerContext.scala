package soc.playerRepository

import soc.game.{CatanMove, GameRules, GameState}
import soc.game.board.CatanBoard
import soc.game.inventory.{Inventory, InventoryHelperFactory}
import soc.game.player.PlayerStateHelper
import soc.game.player.moveSelector.MoveSelector
import soc.storage.GameId

import scala.collection.mutable.Map
import scala.concurrent.Future

class PlayerContext[GAME <: Inventory[GAME], T <: Inventory[T]](val name: String, val moveSelector: MoveSelector[GAME, T])(implicit factory: InventoryHelperFactory[T], gameRules: GameRules) {

  private val cache: Map[(GameId, Int), GameState[T]] = Map.empty

  def updateGameState(gameId: GameId, position: Int)(f: GameState[_] => GameState[_]): Unit = this.synchronized{
    cache((gameId, position)) = f(cache(gameId, position)).asInstanceOf[GameState[T]]
  }

  def addGameState(gameId: GameId, position: Int, board: CatanBoard, players: Seq[(String, Int)]) = this.synchronized(cache((gameId, position)) = GameState[T](board, players, gameRules))
  def getGameState(gameId: GameId, position: Int): GameState[T] = this.synchronized(cache((gameId, position)))
  def removeGameState(gameId: GameId, position: Int): Option[GameState[T]] = this.synchronized{cache.remove((gameId, position))}
  def numGames = cache.size
  def gameIds = cache.keys.toSeq

  def getInitialPlacementMove(gameId: GameId, position: Int, inventory: GAME, id: Int, first: Boolean): Future[CatanMove] = {
     moveSelector.initialPlacementMove(getGameState(gameId, position), inventory, id)(first)
  }

  def getDiscardCardMove(gameId: GameId, position: Int, inventory: GAME, id: Int): Future[CatanMove] = {
    moveSelector.discardCardsMove(getGameState(gameId, position), inventory, id)
  }

  def getMoveRobberAndStealMove(gameId: GameId, position: Int, inventory: GAME, id: Int): Future[CatanMove] = {
    moveSelector.moveRobberAndStealMove(getGameState(gameId, position), inventory, id)
  }

  def getMove(gameId: GameId, position: Int, inventory: GAME, id: Int): Future[CatanMove] = {
    moveSelector.turnMove(getGameState(gameId, position), inventory, id)
  }
}



