package soc.game.player.moveSelector

import soc.game.GameState
import soc.game.CatanMove
import soc.game.inventory.Inventory
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.player.PlayerStateHelper

import scala.concurrent.Future

trait MoveSelector[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]] {

  def initialPlacementMove(gameState: GameState[PLAYER], inventory: GAME, position: Int)(first: Boolean): CatanMove

  def discardCardsMove(gameState: GameState[PLAYER],inventory: GAME, position: Int): CatanMove

  def moveRobberAndStealMove(gameState: GameState[PLAYER],inventory: GAME, position: Int): CatanMove

  def turnMove(gameState: GameState[PLAYER],inventory: GAME, position: Int): CatanMove

}
