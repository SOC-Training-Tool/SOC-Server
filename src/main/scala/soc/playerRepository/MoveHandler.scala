package soc.playerRepository

import soc.behaviors.messages.RequestMessage.MoveRequest
import soc.moves.CatanMove
import soc.inventory.Inventory

import scala.concurrent.Future

trait MoveHandler[GAME <: Inventory[GAME]] {

  def getMoveResponse(request: MoveRequest[GAME]): Future[CatanMove]

}