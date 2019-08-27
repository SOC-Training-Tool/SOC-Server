package soc.storage

import org.scalatest.{FunSpec, Matchers}
import soc.game._
import soc.game.Roll
import soc.game.board.Vertex
import soc.game.inventory.Brick
import soc.game.inventory.resources.CatanResourceSet

class MoveEntrySpec extends FunSpec with Matchers {

  describe("json encoder") {

    val moveEntryA = MoveEntry(0, 0, 0, 0, RollResult(Roll(6)))
    val moveEntryB = MoveEntry(1, 1, 1, 1, BuildCityMove(Vertex(0)))
    val moveEntryC = MoveEntry(1, 1, 1, 1, DiscardResourcesMove(Map(0 -> CatanResourceSet(Brick))))

    val l: List[MoveEntry] = List(moveEntryA, moveEntryB, moveEntryC)

    val moveResult: MoveResult = RollResult(Roll(6))

    it("should be a json") {
      import io.circe.syntax._
      import CatanMove._
      import MoveEntry._
      //import io.circe.generic.auto._

      println(l.asJson)

    }

  }

}
