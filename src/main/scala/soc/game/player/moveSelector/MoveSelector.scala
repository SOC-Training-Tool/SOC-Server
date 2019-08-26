package soc.game.player.moveSelector

import soc.game.GameState
import soc.game.CatanMove
import soc.game.inventory.Inventory
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.player.PlayerStateManager

import scala.concurrent.Future

trait MoveSelector[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]] {

  def initialPlacementMove(gameState: GameState[PLAYER], inventory: GAME, position: Int)(first: Boolean): Future[CatanMove.Move]

  def discardCardsMove(gameState: GameState[PLAYER],inventory: GAME, position: Int): Future[CatanMove.Move]

  def moveRobberAndStealMove(gameState: GameState[PLAYER],inventory: GAME, position: Int): Future[CatanMove.Move]

  def turnMove(gameState: GameState[PLAYER],inventory: GAME, position: Int): Future[CatanMove.Move]

}
