package soc.akka

import akka.actor.typed.SupervisorStrategy.Stop
import akka.actor.typed.scaladsl.Behaviors
import soc.akka.messages.RequestMessage._
import soc.akka.messages.{GameMessage, Response, Terminate, UpdateMessage}
import soc.akka.messages.{GameMessage, Response, Terminate}
import soc.game.player.moveSelector.MoveSelector

object PlayerBehavior {


  def playerBehavior(moveSelector: MoveSelector) = Behaviors.receiveMessage[GameMessage] {
    case _: UpdateMessage => Behaviors.same

    case InitialPlacementRequest(state, id, first, ref) =>
      ref ! Response(id, moveSelector.initialPlacementMove(state, id)(first))
      Behaviors.same
    case DiscardCardRequest(state, id, ref) =>
      ref ! Response(id, moveSelector.discardCardsMove(state, id))
      Behaviors.same
    case MoveRobberRequest(state, id, ref) =>
      ref ! Response(id, moveSelector.moveRobberAndStealMove(state, id))
      Behaviors.same
    case MoveRequest(state, id, ref) =>
      ref ! Response(id, moveSelector.turnMove(state, id))
      Behaviors.same

    case Terminate =>
      Behaviors.stopped
  }
}
