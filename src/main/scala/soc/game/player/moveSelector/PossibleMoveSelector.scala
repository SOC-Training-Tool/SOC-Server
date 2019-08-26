package soc.game.player.moveSelector

import soc.akka.Main.random
import soc.game.{CatanMove, GameState, Roll}
import soc.game.CatanMove.RollDiceMove
import soc.game.inventory.Inventory
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.player.CatanPossibleMoves

import scala.concurrent.Future

case class PossibleMoveSelector[T <: Inventory[T]](select: (GameState[T], Iterator[CatanMove.Move]) => CatanMove.Move) extends MoveSelector[PerfectInfo, T] {

  override def initialPlacementMove(gameState: GameState[T], inventory: PerfectInfo, position: Int)(first: Boolean): Future[CatanMove.Move] = {
    val moves = CatanPossibleMoves(gameState, inventory, position).getPossibleInitialPlacements(first).toIterator
    Future.successful(select(gameState, moves))
  }

  override def discardCardsMove(gameState: GameState[T], inventory: PerfectInfo, position: Int): Future[CatanMove.Move] = {
    val moves = CatanPossibleMoves(gameState, inventory, position).getPossibleDiscards().toIterator
    Future.successful(select(gameState, moves))
  }

  override def moveRobberAndStealMove(gameState: GameState[T], inventory: PerfectInfo, position: Int): Future[CatanMove.Move] = {
    val moves = CatanPossibleMoves(gameState, inventory, position).getPossibleRobberLocations.toIterator
    Future.successful(select(gameState, moves))

  }

  override def turnMove(gameState: GameState[T], inventory: PerfectInfo, position: Int): Future[CatanMove.Move] = {
   val moves =  CatanPossibleMoves(gameState, inventory, position).getPossibleMovesForState.toIterator
    Future.successful(select(gameState, moves))
  }
}

object PossibleMoveSelector {

  def randSelector[T <: Inventory[T]] = PossibleMoveSelector[T] { case (_, moves: Iterator[CatanMove.Move]) =>
    val (a, b) = moves.duplicate
    b.drop(random.nextInt(a.length)).next()
  }

//  def stateEvaluatorByMax(eval: GameState => Double) = PossibleMoveSelector { case (state, moves) =>
//
//      moves.map {
//        case RollDiceMove =>
//          (2 to 12).map { r =>
//            val roll = Roll(r)
//
//
//          }
//
//
//      }
//
//
//
//
//
//  }
//
//
//
//
//  def stateEvaluatorByMin(eval: GameState => Double) =>

}
