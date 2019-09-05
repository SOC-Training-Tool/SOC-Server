package soc.playerRepository

import soc.game.{GameRules, GameState}
import soc.game.board.CatanBoard
import soc.game.inventory.{Inventory, InventoryHelperFactory}
import soc.game.player.PlayerStateHelper
import soc.game.player.moveSelector.MoveSelector

import scala.collection.mutable.Map

class PlayerContext[GAME <: Inventory[GAME], T <: Inventory[T]](val name: String, val moveSelector: MoveSelector[GAME, T])(implicit factory: InventoryHelperFactory[T], gameRules: GameRules) {

  private val cache: Map[Int, GameState[T]] = Map.empty

  def updateGameState(gameId: Int)(f: GameState[T] => GameState[T]): Unit = {
    cache(gameId) = f(cache(gameId))
  }

  def addGameState(gameId: Int, board: CatanBoard, players: Seq[(String, Int)]) = cache(gameId) = GameState[T](board, PlayerStateHelper[T](players))
  def getGameState(gameId: Int) = cache(gameId)
  def removeGameState(gameId: Int) = cache.remove(gameId)
  def numGames = cache.size
  def gameIds = cache.keys.toSeq
}



