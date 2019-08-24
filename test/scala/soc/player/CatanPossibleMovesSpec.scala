package soc.player

import log.Log
import org.scalatest.{FunSpec, Matchers}
import soc.CatanFixtures
import soc.game.board.{Edge, Vertex}
import soc.game.player.{CatanPossibleMoves, PerfectInfoPlayerState}
import soc.game.{CatanSet, GameState, Roll, Settlement}
import soc.game.CatanMove._
import soc.game.resources.{CatanResourceSet, Gain}

class CatanPossibleMovesSpec extends FunSpec with Matchers {

  val stubbedLog = new Log {override def print(message: String): Unit = ()}
  val singleHexBoard = CatanFixtures.singleHexBoard
  val players = PerfectInfoPlayerState("player1",  0)

  val gameState = GameState(singleHexBoard, List(players))

  describe("getPossibleInitialPlacements") {

    it("should generate a list of possible initial placements") {

      val moves: Seq[InitialPlacementMove] = CatanPossibleMoves(gameState, 0).getPossibleInitialPlacements(true)
      moves should not be empty
    }
  }

  describe ("getPossibleMovesForState") {

    it("should only return Roll Dice move if dice have not been rolled and players have no cards") {
      val moves = CatanPossibleMoves(gameState, 0).getPossibleMovesForState
      moves should contain only RollDiceMove
    }

  }

  describe("getPossibleDiscards") {

    it("") {
      val a =  CatanSet.toList(CatanResourceSet(2, 2, 1, 0, 3))
      println(a.length)
      println(a.combinations(4))
      
      
      val b: Seq[TraversableOnce[Int]] = List(List(1, 2, 3), Iterator(1, 2, 3))
      
    }

    it("should include one resource player has 8 of same resource") {
      val playerWithResources = players.copy(resourceSet = CatanResourceSet(8, 0, 0, 0, 0))
      val gameState = GameState(singleHexBoard, List(playerWithResources))

      val moves = CatanPossibleMoves(gameState, 0).getPossibleDiscards().toList
      println(moves)

      moves should contain(DiscardResourcesMove(CatanResourceSet(4, 0, 0, 0, 0)))
    }

  }

}
