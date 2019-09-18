package soc.playerRepository

import soc.akka.messages.RequestMessage.MoveRequest
import soc.game.CatanMove
import soc.game.inventory.Inventory

import scala.concurrent.Future

trait MoveHandler[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]] {

  def getMoveResponse(request: MoveRequest[GAME, PLAYER]): Future[CatanMove]

}