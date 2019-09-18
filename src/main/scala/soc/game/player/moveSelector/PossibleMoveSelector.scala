package soc.game.player.moveSelector

import soc.game.{CatanMove, GameState}
import soc.game.inventory.Inventory
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.player.CatanPossibleMoves

import scala.concurrent.Future
import scala.util.Random

case class PossibleMoveSelector[T <: Inventory[T]](select: (GameState[T], Iterator[CatanMove]) => CatanMove) extends MoveSelector[PerfectInfo, T] {

  override def initialPlacementMove(gameState: GameState[T], inventory: PerfectInfo, position: Int)(first: Boolean): Future[CatanMove] = {
    val moves = CatanPossibleMoves(gameState, inventory, position).getPossibleInitialPlacements(first).toIterator
    Future.successful(select(gameState, moves))
  }

  override def discardCardsMove(gameState: GameState[T], inventory: PerfectInfo, position: Int): Future[CatanMove] = {
    val moves = CatanPossibleMoves(gameState, inventory, position).getPossibleDiscards().toIterator
    Future.successful(select(gameState, moves))
  }

  override def moveRobberAndStealMove(gameState: GameState[T], inventory: PerfectInfo, position: Int): Future[CatanMove] = {
    val moves = CatanPossibleMoves(gameState, inventory, position).getPossibleRobberLocations.toIterator
    Future.successful(select(gameState, moves))

  }

  override def turnMove(gameState: GameState[T], inventory: PerfectInfo, position: Int): Future[CatanMove] = {
    val moves =  CatanPossibleMoves(gameState, inventory, position).getPossibleMovesForState.toIterator
    Future.successful(select(gameState, moves))
  }
}

object PossibleMoveSelector {

  def randSelector[T <: Inventory[T]](implicit random: Random) = PossibleMoveSelector[T] { case (_, moves: Iterator[CatanMove]) =>
    val (a, b) = moves.duplicate
    b.drop(random.nextInt(a.length)).next()
  }
}
