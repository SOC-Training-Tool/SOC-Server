package soc.game.player.moveSelector

import soc.game.player.{CatanPossibleMoves, PerfectInfoPlayerState}
import soc.game.GameState
import soc.game.CatanMove

case class PossibleMoveSelector(select: Iterator[CatanMove] => CatanMove) extends MoveSelector {

  override def initialPlacementMove(gameState: GameState, position: Int)(first: Boolean): CatanMove = {
    val moves = CatanPossibleMoves(gameState, position).getPossibleInitialPlacements(first)
    select(moves.toIterator)
  }

  override def discardCardsMove(gameState: GameState, position: Int): CatanMove = {
    val moves = CatanPossibleMoves(gameState, position).getPossibleDiscards()
    select(moves)
  }

  override def moveRobberAndStealMove(gameState: GameState, position: Int): CatanMove = {
    select(CatanPossibleMoves(gameState, position).getPossibleRobberLocations.toIterator)
  }

  override def turnMove(gameState: GameState, position: Int): CatanMove = {
   val moves =  CatanPossibleMoves(gameState, position).getPossibleMovesForState.toIterator
    select(moves)

  }
}
