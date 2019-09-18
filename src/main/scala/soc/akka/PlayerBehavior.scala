package soc.akka

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import soc.akka.messages.RequestMessage._
import soc.akka.messages.{GameMessage, MoveResponse, PlayerDoMove, UpdateMessage}
import soc.akka.messages.UpdateMessage._
import soc.game.GameState
import soc.game.inventory.Inventory
import soc.playerRepository.PlayerContext

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object PlayerBehavior {

  def playerBehavior[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](playerContext: PlayerContext[GAME, PLAYER], position: Int)(implicit ec: ExecutionContext): Behavior[GameMessage] = Behaviors.setup[GameMessage] { context =>

      Behaviors.receiveMessage {

        case StartGame(gameId, state: GameState[PLAYER]) =>
          playerContext.sendInitialGameState(gameId, position, state)
          Behaviors.same

        case MoveResultUpdate(gameId, result) =>
          playerContext.updateGameState(gameId, position, result)
          Behaviors.same

        case _: UpdateMessage =>
          Behaviors.same

        case request: MoveRequest[GAME, PLAYER] =>
          context.pipeToSelf(playerContext.getMoveResponse(request)) {
            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
            case Failure(ex) => null
          }
          Behaviors.same

//        case request: InitialPlacementRequest[GAME, PLAYER] =>
//          context.pipeToSelf(playerContext.getInitialPlacementMove(request.gameId, position, request.inventory, request.playerId, request.first)) {
//            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
//            case Failure(ex) => null
//          }
//          Behaviors.same
//
//        case request: DiscardCardRequest[GAME, PLAYER] =>
//          context.pipeToSelf(playerContext.getDiscardCardMove(request.gameId, position, request.inventory, request.playerId)) {
//            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
//            case Failure(ex) => null
//          }
//          Behaviors.same
//
//        case request: MoveRobberRequest[GAME, PLAYER] =>
//          context.pipeToSelf(playerContext.getMoveRobberAndStealMove(request.gameId, position, request.inventory, request.playerId)) {
//            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
//            case Failure(ex) => null
//          }
//          Behaviors.same
//
//        case request: MoveRequest[GAME, PLAYER] =>
//          context.pipeToSelf(playerContext.getMove(request.gameId, position, request.inventory, request.playerId)) {
//            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
//            case Failure(ex) => null
//          }
//          Behaviors.same

        case PlayerDoMove(playerId, move, respondTo) =>
          respondTo ! MoveResponse(playerId, move)
          Behaviors.same


        case _ => Behaviors.stopped
    }
  }
}
