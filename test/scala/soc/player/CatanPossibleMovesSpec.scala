//package soc.player
//
//import log.Log
//import org.scalatest.{FunSpec, Matchers}
//import soc.CatanFixtures
//import soc.game.board.{Edge, Vertex}
//import soc.game.player.{CatanPossibleMoves, PlayerState, PlayerStateManager}
//import soc.game.CatanMove._
//import soc.game.GameState
//import soc.game.inventory.CatanSet
//import soc.game.inventory.Inventory.{PerfectInfo}
//import soc.game.inventory.resources.CatanResourceSet
//
//class CatanPossibleMovesSpec extends FunSpec with Matchers {
//
//  val singleHexBoard = CatanFixtures.singleHexBoard
//
//  val gameState = GameState[PerfectInfo](singleHexBoard, PlayerStateManager[PerfectInfo](List(("player1", 0))))
//
//  describe("getPossibleInitialPlacements") {
//
//    it("should generate a list of possible initial placements") {
//
//      val moves: Seq[InitialPlacementMove] = CatanPossibleMoves(gameState, 0).getPossibleInitialPlacements(true)
//      moves should not be empty
//    }
//  }
//
//  describe ("getPossibleMovesForState") {
//
//    it("should only return Roll Dice move if dice have not been rolled and players have no cards") {
//      val moves = CatanPossibleMoves(gameState, 0).getPossibleMovesForState
//      moves should contain only RollDiceMove
//    }
//
//  }
//
//  describe("getPossibleDiscards") {
//
//    it("") {
//      val a =  CatanSet.toList(CatanResourceSet(2, 2, 1, 0, 3))
//      println(a.length)
//      println(a.combinations(4))
//
//
//      val b: Seq[TraversableOnce[Int]] = List(List(1, 2, 3), Iterator(1, 2, 3))
//
//    }
//
//    it("should include one resource player has 8 of same resource") {
//
//    }
//
//  }
//
//}
