//package soc
//
//import log.Log
//import org.scalatest.{FunSpec, Matchers}
//import soc.game.GameState
//import soc.game.board.{Edge, Vertex}
//import soc.game.inventory.Inventory.PerfectInfo
//import soc.game.inventory._
//import soc.game.inventory.resources.{CatanResourceSet, Gain}
//import soc.game.player.{PlayerState, PlayerStateManager}
//
//class GameStateSpec extends FunSpec with Matchers {
//
//
//  val singleHexBoard = CatanFixtures.singleHexBoard
//  val playerList = List(
//    ("player1", 0),
//    ("player2", 1),
//    ("player3", 2),
//    ("player4", 3),
//  )
//
//  val stubbedLog = new Log {override def print(message: String): Unit = ()}
//  val gameState = GameState[PerfectInfo](singleHexBoard, PlayerStateManager(playerList))
//
//  describe("initialPlacement") {
//
//    it("should add settlement and road to board if first placement") {
//      val result = gameState.initialPlacement(0, true, Vertex(0), Edge(Vertex(0), Vertex(1)))
//      result.board.verticesBuildingMap should contain only (Vertex(0) -> Settlement(0))
//      result.board.edgesBuildingMap should contain only (Edge(Vertex(0), Vertex(1)) -> Road(0))
//    }
//
//    it("should add settlement and road to board and gain resources from hex if second placement") {
//      val expectedGainedSet = CatanResourceSet(0, 0, 0, 0, 1)
//
//      val result = gameState.initialPlacement(0, false, Vertex(0), Edge(Vertex(0), Vertex(1)))
//      result.board.verticesBuildingMap should contain only (Vertex(0) -> Settlement(0))
//      result.board.edgesBuildingMap should contain only (Edge(Vertex(0), Vertex(1)) -> Road(0))
//
//      result.bank shouldEqual gameState.bank.subtract(expectedGainedSet)
//      result.transactions should contain only Gain(0, expectedGainedSet)
//      //result.possibleHands.hands should have length 1
//      //result.possibleHands.hands.head should contain only (0 -> expectedGainedSet)
//    }
//  }
//
//  describe("rollDice") {
//
//  }
//
//  describe("buyDevelopmentCards") {
//
//  }
//}
