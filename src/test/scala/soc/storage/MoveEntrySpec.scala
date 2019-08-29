package soc.storage

import io.circe.Encoder
import org.scalatest.{FunSpec, Matchers}
import soc.game._
import soc.game.Roll
import soc.game.board.Vertex
import soc.game.inventory.Brick
import soc.game.inventory.resources.CatanResourceSet
import soc.game.moves.{BuildCityMove, DiscardResourcesMove, RollResult}

class MoveEntrySpec extends FunSpec with Matchers {

  describe("json encoder") {

    val moveEntryA = MoveEntry(0, 0, 0, 0, RollResult(Roll(6)))
    val moveEntryB = MoveEntry(1, 1, 1, 1, BuildCityMove(Vertex(0)))
    val moveEntryC = MoveEntry(1, 1, 1, 1, DiscardResourcesMove(Map(0 -> CatanResourceSet(Brick))))

    val l: List[MoveEntry] = List(moveEntryA, moveEntryB, moveEntryC)

    val moveResult = RollResult(Roll(6))

    it("should be a json") {
      import io.circe.syntax._

//
//      implicit val encoderMoveEntry: Encoder[MoveEntry] = Encoder.forProduct5("gameId", "moveNumber", "playerId", "playerOrderNumber", "move"){ me =>
//        (me.gameId, me.moveNumber, me.playerId, me.playerOrderNumber, me.move)
//      }

      //println(moveEntryB.asJson)

    }

  }

}
