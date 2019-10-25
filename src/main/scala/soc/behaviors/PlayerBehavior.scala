package soc.behaviors

import akka.actor.Scheduler
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import protos.soc.game.MoveResponse
import soc.behaviors.messages.{GameMessage, PlayerDoMove, RequestMessage, ResultResponse, UpdateMessage, MoveResponse => ServerMoveResponse}
import soc.behaviors.messages.UpdateMessage._
import soc.inventory.Inventory
import soc.inventory.Inventory.PublicInfo
import soc.playerRepository.PlayerContext
import soc.state.GameState

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object PlayerBehavior {

  def playerBehavior[GAME <: Inventory[GAME]](playerContext: PlayerContext[GAME], position: Int)
    (implicit ec: ExecutionContext, timeout: Timeout, schedule: Scheduler): Behavior[GameMessage] = Behaviors.setup[GameMessage] { context =>

      Behaviors.receiveMessage {

        case StartGame(gameId, state: GameState[PublicInfo]) =>
          //playerContext.sendInitialGameState(gameId, position, state)
          context.log.debug(s"receivedStartGame")
          Behaviors.same

        case GameOver(gameId, msg) =>
          //playerContext.sendGameOver(gameId, position, msg)
          Behaviors.stopped

        case MoveResultUpdate(gameId, result) =>
          playerContext.updateGameState(gameId, position, result)
          Behaviors.same

        case _: UpdateMessage =>
          Behaviors.same

        case request: RequestMessage[GAME] =>
          context.log.debug(s"received request $request")
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

        case PlayerDoMove(playerId, receievedMove, respondTo) =>
          val clientMoveResponse: Future[MoveResponse] = respondTo ? (replyTo => (ServerMoveResponse(playerId, receievedMove.move, replyTo)))
          clientMoveResponse.map(receievedMove.moveResponse.success(_))

          Behaviors.same


        case _ => Behaviors.stopped
    }
  }
}
