package soc.game.player.moveSelector

import soc.game.GameState
import soc.game.CatanMove

trait MoveSelector {

  def initialPlacementMove(gameState: GameState, position: Int)(first: Boolean): CatanMove.Move

  def discardCardsMove(gameState: GameState, position: Int): CatanMove.Move

  def moveRobberAndStealMove(gameState: GameState, position: Int): CatanMove.Move

  def turnMove(gameState: GameState, position: Int): CatanMove.Move

}
