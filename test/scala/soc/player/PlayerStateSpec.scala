package soc.player

import org.scalatest.{FunSpec, Matchers}
import soc.CatanFixtures
import soc.game.{Misc, Roll}
import soc.game.board.Vertex
import soc.game.player.PlayerState
import soc.game.resources.CatanResourceSet

trait PlayerStateSpec extends FunSpec with Matchers {

  def getPlayer(name: String, position: Int): PlayerState
  val singleHexBoard = CatanFixtures.singleHexBoard

  private val player = getPlayer("", 0)

  describe("buildSettlement") {

    it("should add vertex to settlement list") {
      val newPlayer = player.buildSettlement(singleHexBoard, Vertex(0))
      newPlayer.settlements should contain only Vertex(0)
    }

    it("should increment board points") {
      val newPlayer = player.buildSettlement(singleHexBoard, Vertex(0))
      newPlayer.boardPoints shouldBe 1
    }

    it("should increment ports when settlement is on port") {
      val newPlayer = player.buildSettlement(singleHexBoard, Vertex(0))
      newPlayer.ports should contain only Misc
    }

    it("should add dots from hex settlement is on") {
      val newPlayer = player.buildSettlement(singleHexBoard, Vertex(0))
      newPlayer.dots shouldBe CatanResourceSet(0, 0, 0, 0, 5)
    }

  }

}
