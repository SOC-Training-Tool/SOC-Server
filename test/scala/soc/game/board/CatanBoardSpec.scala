package soc.game.board

import org.scalatest.{FunSpec, Matchers}
import soc.game.board._
import soc.game.{Misc, Port, ResourceHex, Roll, Wood}

class CatanBoardSpec extends FunSpec with Matchers {

  private val vertexMap = Map(0 -> List(0, 1, 2, 3, 4, 5))
  private def portMapFunc(ports: List[Port]) = Map(0 -> ports(0), 1 -> ports(0))
  private val hexes = List(ResourceHex(Wood, Roll(6)))
  private val ports = List(Misc)

  val singleHexBoard = CatanBoard(vertexMap, portMapFunc, hexes, ports)

  describe("canBuildSettlement") {

    describe("should return false when") {
      it("not valid vertex") {
          singleHexBoard.canBuildSettlement(Vertex(7), 0, true) shouldBe false
      }

      it("there already exists a building on that vertex") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 0)
        board.canBuildSettlement(Vertex(0), 0, true) shouldBe false
      }

      it ("a neighboring vertex contains a building") {
        val board = singleHexBoard.buildSettlement(Vertex(1), 0)
        board.canBuildSettlement(Vertex(0), 0, true) shouldBe false
      }

      describe ("not an initial placement") {

        it("and no road is adjacent to vertex") {
           singleHexBoard.canBuildSettlement(Vertex(0), 0, false) shouldBe false
        }

        it("and only road is adjacent to vertex is with incorrect playerId") {
          val board = singleHexBoard.buildRoad(Edge(Vertex(0), Vertex(1)), 1)
          board.canBuildSettlement(Vertex(0), 0, false) shouldBe false
        }
      }
    }

    describe ("should return true when it is a valid vertex, no neighboring vertex contains a building,") {

      it("and it is an initial settlement") {
        singleHexBoard.canBuildSettlement(Vertex(0), 0, true) shouldBe true
      }

      it ("and it is not an initial settlement but there is an adjacent road with the correct playerId ") {
        val board = singleHexBoard.buildRoad(Edge(Vertex(0), Vertex(1)), 0)
        board.canBuildSettlement(Vertex(0), 0, false) shouldBe true
      }

      it("even if there are settlements at least two vertices away") {
        val board = singleHexBoard
          .buildSettlement(Vertex(2), 0)
          .buildSettlement(Vertex(4), 1)
        board.canBuildSettlement(Vertex(0), 0, true) shouldBe true
      }
    }
  }

  describe("buildSettlement") {

  }

  describe("canBuildCity") {

    describe("should return false when") {

      it("there is not a settlement on the vertex") {
        singleHexBoard.canBuildCity(Vertex(0), 0) shouldBe false
      }

      it("there is a settlement on the vertex with a different owner") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 1)
        board.canBuildCity(Vertex(0), 0) shouldBe false
      }

      it("there is already a city on the vertex") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 0)
          .buildCity(Vertex(0), 0)
        board.canBuildCity(Vertex(0), 0) shouldBe false
      }

    }

    it ("should return true when there is a settlement with the same owner on the vertex") {
      val board = singleHexBoard.buildSettlement(Vertex(0), 0)
      board.canBuildCity(Vertex(0), 0) shouldBe true
    }

  }

  describe("buildCity") {

  }

  describe("canBuildRoad") {

    val edge = Edge(Vertex(0), Vertex(1))

    describe ("should return false when") {
      it("not a valid edge") {
        singleHexBoard.canBuildRoad(Edge(Vertex(-1), Vertex(0)), 0) shouldBe false
        singleHexBoard.canBuildRoad(Edge(Vertex(0), Vertex(2)), 0) shouldBe false
      }

      it("there is already a road on that edge") {

        val board = singleHexBoard.buildRoad(edge, 0)
        board.canBuildRoad(edge, 0) shouldBe false
      }

      it("there is building on one of its vertices with a different owner") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 1)
        board.canBuildRoad(edge, 0) shouldBe false
      }

      it("there are no building on its vertices and no neighboring roads") {
        singleHexBoard.canBuildRoad(edge, 0) shouldBe false
      }

      it("there are no building on its vertices and neighboring roads with different owners") {
        val neighborEdge = Edge(Vertex(1), Vertex(2))
        val board = singleHexBoard.buildRoad(neighborEdge, 1)
        board.canBuildRoad(edge, 0)
      }
    }

    describe ("should return true when") {

      it("there is a settlement with same owner on its vertex") {
        val board = singleHexBoard.buildSettlement(Vertex(0), 0)
        board.canBuildRoad(edge, 0) shouldBe true
      }

      it("there is a neighboring road with the same owner") {
        val board = singleHexBoard.buildRoad(Edge(Vertex(1), Vertex(2)), 0)
        board.canBuildRoad(edge, 0) shouldBe true
      }
    }
  }

  describe("buildRoad") {

  }

  describe("getPossibleSettlements") {

    describe("when initial") {
      it("should return all vertices if board is empty") {
        singleHexBoard.getPossibleSettlements(0, true) should contain allElementsOf(singleHexBoard.vertices)
      }

      it("should return vertices at least 2 edges away if there is already a settlement on the board") {
        val occupiedVertices = List(5, 0, 1).map(Vertex)
        val board = singleHexBoard.buildSettlement(Vertex(0), 1)
        board.getPossibleSettlements(0, true) should contain allElementsOf(board.vertices.filterNot(occupiedVertices.contains))
      }
    }
  }

  describe("getPossibleCities") {

  }

  describe("getPossibleRoads") {

  }

  describe("playersOnHex") {

  }

  describe("getResourcesGainedOnRoll") {

  }

  describe("longestRoadLength") {


    val vertexMap = Map(
      0 -> List(0, 1, 2, 3, 4, 5),
      1 -> List(2, 6, 7, 8, 9, 3),
      2 -> List(10, 11, 12, 13, 7, 6)
    )

    def portMapFunc(ports: List[Port]) = Map.empty[Int, Port]

    val hexes = List(ResourceHex(Wood, Roll(6)), ResourceHex(Wood, Roll(6)), ResourceHex(Wood, Roll(6)))
    val ports = List.empty

    val roadBoard = CatanBoard(vertexMap, portMapFunc, hexes, ports)

    describe("without settlements") {
      it("road scenario1") {
        roadBoard.buildRoad(Edge(Vertex(2), Vertex(6)), 0).longestRoadLength(0) shouldBe 1
      }

      it("road scenario2") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .longestRoadLength(0) shouldBe 2
      }

      it("road scenario3") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(6), Vertex(10)), 0)
          .longestRoadLength(0) shouldBe 2
      }

      it("road scenario4") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .longestRoadLength(0) shouldBe 2
      }

      it("road scenario5") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 2
      }

      it("road scenario6") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }

      it("road scenario7") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }

      it("road scenario8") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }

      it("road scenario9") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }

      it("road scenario10") {
        roadBoard
          .buildRoad(Edge(Vertex(2), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(1), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(3), Vertex(2)), 0)
          .buildRoad(Edge(Vertex(7), Vertex(6)), 0)
          .buildRoad(Edge(Vertex(10), Vertex(6)), 0)
          .longestRoadLength(0) shouldBe 3
      }
    }
  }
}
