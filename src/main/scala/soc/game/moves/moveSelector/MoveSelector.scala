package soc.game.moves.moveSelector

import soc.game.GameState
import soc.game.inventory.Inventory
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.moves.CatanMove
import soc.game.player.PlayerStateManager

import scala.concurrent.Future

trait MoveSelector[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]] {

  def initialPlacementMove(gameState: GameState[PLAYER], inventory: GAME, position: Int)(first: Boolean): Future[CatanMove]

  def discardCardsMove(gameState: GameState[PLAYER],inventory: GAME, position: Int, currentTurn: Int): Future[CatanMove]

  def moveRobberAndStealMove(gameState: GameState[PLAYER],inventory: GAME, position: Int): Future[CatanMove]

  def turnMove(gameState: GameState[PLAYER],inventory: GAME, position: Int): Future[CatanMove]

}
