package soc.storage

import io.circe.Encoder
import org.scalatest.{FunSpec, Matchers}
import soc.board.Vertex
import soc.core.Roll
import soc.moves._

class MoveEntrySpec extends FunSpec with Matchers {

  describe("json encoder") {

    val gId = GameId(SimulatedGame, "", 0)

    val moveEntryA = MoveEntry(gId, 0, 0, 0, RollResult(Roll(6)))
    val moveEntryB = MoveEntry(gId, 1, 1, 1, BuildCityMove(Vertex(0)))

    val l: List[MoveEntry] = List(moveEntryA, moveEntryB)

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
