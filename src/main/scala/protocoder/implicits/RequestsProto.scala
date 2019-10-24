package protocoder.implicits

import protocoder.ProtoCoder
import protocoder.ProtoCoder.ops._
import StateProto._
import ResourceProto._
import protos.soc.game.ActionRequest
import protos.soc.game.ActionRequest.ActionRequestType._
import soc.behaviors.messages.RequestMessage
import soc.behaviors.messages.RequestMessage._
import soc.inventory.Inventory

object RequestsProto {

  implicit def perfectRequestsProto[GAME <: Inventory[GAME]]: ProtoCoder[RequestMessage[GAME], ActionRequest] = request =>
    request match {
      case InitialPlacementRequest(_, playerState, inventory, _, _, _) =>
        ActionRequest(PLACE_INITIAL_SETTLEMENT, playerState.proto, inventory.proto)
      case DiscardCardRequest(_, playerState, inventory, _, _) =>
        ActionRequest(DISCARD, playerState.proto, inventory.proto)
      case MoveRobberRequest(_, playerState, inventory, _, _) =>
        ActionRequest(MOVE_ROBBER, playerState.proto, inventory.proto)
      case MoveRequest(_, playerState, inventory, _, _) if playerState.canRollDice =>
        ActionRequest(START_TURN, playerState.proto, inventory.proto)
      case MoveRequest(_, playerState, inventory, _, _) =>
        ActionRequest(BUILD_OR_TRADE_OR_PLAY_OR_PASS, playerState.proto, inventory.proto)
    }

}
