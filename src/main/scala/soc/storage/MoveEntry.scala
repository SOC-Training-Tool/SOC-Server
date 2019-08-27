package soc.storage

import io.circe.Encoder
import io.circe.generic.semiauto._

import soc.game.MoveResult

case class MoveEntry (
  gameId: Int,
  moveNumber: Int,
  playerId: Int,
  playerOrderNumber: Int,
  move: MoveResult,
)

object MoveEntry {

  import soc.game.CatanMove._
  import soc.game.inventory.resources.CatanResourceSet._


  implicit val moveEntryEncoder: Encoder[MoveEntry] = deriveEncoder

}

