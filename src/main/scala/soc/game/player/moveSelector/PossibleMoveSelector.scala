package soc.game.player.moveSelector

import soc.akka.Main.random
import soc.game.player.{CatanPossibleMoves, PerfectInfoPlayerState}
import soc.game.{CatanMove, GameState, Roll}
import soc.game.CatanMove.RollDiceMove

case class PossibleMoveSelector(select: (GameState, Iterator[CatanMove]) => CatanMove) extends MoveSelector {

  override def initialPlacementMove(gameState: GameState, position: Int)(first: Boolean): CatanMove = {
    val moves = CatanPossibleMoves(gameState, position).getPossibleInitialPlacements(first)
    select(gameState, moves.toIterator)
  }

  override def discardCardsMove(gameState: GameState, position: Int): CatanMove = {
    val moves = CatanPossibleMoves(gameState, position).getPossibleDiscards()
    select(gameState, moves)
  }

  override def moveRobberAndStealMove(gameState: GameState, position: Int): CatanMove = {
    select(gameState, CatanPossibleMoves(gameState, position).getPossibleRobberLocations.toIterator)
  }

  override def turnMove(gameState: GameState, position: Int): CatanMove = {
   val moves =  CatanPossibleMoves(gameState, position).getPossibleMovesForState.toIterator
    select(gameState, moves)

  }
}

object PossibleMoveSelector {

  val randSelector = PossibleMoveSelector { case (_, moves: Iterator[CatanMove]) =>
    val (a, b) = moves.duplicate
    b.drop(random.nextInt(a.length)).next()
  }
  
}
