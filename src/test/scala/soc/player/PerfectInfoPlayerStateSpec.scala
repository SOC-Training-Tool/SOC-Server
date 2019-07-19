package soc.player
import soc.game.board.Vertex
import soc.game.player.{PerfectInfoPlayerState, PlayerState}
import soc.game.resources.{CatanResourceSet, Gain, Lose, PossibleHands, SOCPossibleHands}

class PerfectInfoPlayerStateSpec extends PlayerStateSpec {
  override def getPlayer(name: String, position: Int): PlayerState = PerfectInfoPlayerState(name, position)

  val player = getPlayer("", 0)

  describe("updateResources") {

    it("gaines resource") {
      val transactions = List(Gain(0, CatanResourceSet(1, 0, 0, 0, 0)))

      player.updateResources(transactions).numCards shouldBe 1

    }

    it("gain and lose resource") {
      val transactions = List(Gain(0, CatanResourceSet(3, 2, 0, 0, 0)), Lose(0, CatanResourceSet(2, 1, 0, 0, 0)))

      val p =  player.updateResources(transactions)

      player.updateResources(transactions).numCards shouldBe 2
    }


  }
}
