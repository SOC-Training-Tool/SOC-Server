package soc.behaviors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import soc.behaviors.MoveResultProviderMessage.{GetMoveResultProviderMessage, MoveResultProviderMessage}
import soc.behaviors.messages._
import soc.{GameConfiguration, storage}
import protos.soc.game.{MoveResponse => ProtoMoveResponse}

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContextExecutor
import soc.behaviors.messages.GameMessage
import soc.behaviors.messages.RequestMessage.{DiscardCardRequest, InitialPlacementRequest, MoveRequest, MoveRobberRequest}
import soc.behaviors.messages.UpdateMessage.{GameOver, LargestArmyUpdate, LongestRoadUpdate, MoveResultUpdate, StartGame, TurnUpdate}
import soc.board.BoardConfiguration
import soc.inventory.Inventory
import soc.inventory.resources.{DiscardedCardsMapBuilder, Steal}
import soc.moves._
import soc.playerRepository.ReceiveMoveFromClient
import soc.state.{GamePhase, GameState}

import scala.language.postfixOps

object GameBehavior {

  def gameBehavior[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](config: GameConfiguration[GAME, BOARD]) = Behaviors.setup[StateMessage[GAME]] { context =>

    implicit val timeout: Timeout = 60.seconds
    implicit val scheduler = context.system.scheduler
    implicit val ec: ExecutionContextExecutor = context.executionContext

    var discarding: Option[DiscardedCardsMapBuilder] = None
    var moveNumber = 0
    var gamestate: GameState[GAME] = config.initState

    val firstPlayerId = config.initState.players.firstPlayerId
    val lastPlayerId = config.initState.players.lastPlayerId

    val resultProvider = context.spawn[MoveResultProviderMessage[GAME]](MoveResultProvider.moveResultProvider(config.resultProvider), s"${config.gameId.key}_result_provider")
    val playerRefs: Map[Int, ActorRef[GameMessage]] = config.players.map { case (id, playerContext) =>
      id -> context.spawn[GameMessage](PlayerBehavior.playerBehavior(playerContext, id), s"${config.gameId.key}_${playerContext.name}_$id")
    }

    context.log.info(s"Starting Game ${config.gameId.key}")
    playerRefs.foreach { case (pos, p) => p ! StartGame(config.gameId, config.initState.toPublicGameState) }

    def respond(replyTo: ActorRef[ProtoMoveResponse], response: String) = replyTo ! ProtoMoveResponse(response)

    def respondSuccess(replyTo: ActorRef[ProtoMoveResponse]) = respond(replyTo, "Success")

    def respondFailure(replyTo: ActorRef[ProtoMoveResponse]) = respond(replyTo, "Failure")

    // Send first request for first player's initial placement
    context.ask[RequestMessage[GAME], MoveResponse](playerRefs(firstPlayerId)) { ref =>
      InitialPlacementRequest(config.gameId,
        gamestate.toPublicGameState,
        gamestate.players.getPlayer(firstPlayerId).inventory,
        firstPlayerId,
        true,
        ref)
    } {
      case Success(r@MoveResponse(`firstPlayerId`, InitialPlacementMove(true, _, _), replyTo)) =>
        respondSuccess(replyTo)
        StateMessage[GAME](gamestate, r)
      case Success(MoveResponse(_, _, replyTo)) =>
        respondFailure(replyTo)
        null
      case Success(_) => null
      case Failure(ex) => StateMessage[GAME](gamestate, ErrorMessage(ex))
    }

    def turnMove(updateState: => GameState[GAME]): Behavior[StateMessage[GAME]] = {
      gamestate = updateState
      val id = gamestate.currentPlayer

      if (gamestate.players.getPlayers.exists(_.points >= 10)) {
        context.scheduleOnce(10 millis, context.self, StateMessage(gamestate, EndGame))
      }
      else {
        context.ask[RequestMessage[GAME], MoveResponse](playerRefs(id))(ref => MoveRequest(config.gameId, gamestate.toPublicGameState, gamestate.players.getPlayer(id).inventory, id, ref)) {
          case Success(MoveResponse(`id`, RollDiceMove, replyTo)) if !gamestate.canRollDice =>
            respondFailure(replyTo)
            null
          case Success(MoveResponse(`id`, _: CatanPlayCardMove, replyTo)) if !gamestate.canPlayCard =>
            respondFailure(replyTo)
            null
          case Success(MoveResponse(`id`, EndTurnMove, replyTo)) if gamestate.canRollDice =>
            respondFailure(replyTo)
            null
          case Success(r@MoveResponse(`id`, _: CatanMove, replyTo)) =>
            respondSuccess(replyTo)
            StateMessage(gamestate, r)
          case Success(MoveResponse(_, _, replyTo)) =>
            respondFailure(replyTo)
            null
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initState, ErrorMessage(ex))
        }
      }
      Behaviors.same
    }

    def sendMove(id: Int, moveResult: MoveResult): Unit = {
      config.moveRecorder.map(_ ! MoveEntryMessage(storage.MoveEntry(config.gameId, moveNumber, config.playerIds(id), id, moveResult)))
      moveNumber = moveNumber + 1
    }

    Behaviors.receiveMessage[StateMessage[GAME]] {

      case StateMessage(state, EndGame) =>
        val winner = state.players.getPlayers.find(_.points >= 10)
        config.moveRecorder.map(_ ! SaveGameMessage(config.gameId, config.boardConfiguration, state.players.getPlayers.map(p => (p.name, p.position) -> p.points).toMap))
        val winMsg = s"Player ${winner.get.position} has won game ${config.gameId} with ${winner.get.points} points and ${state.turn} rolls ${state.players.getPlayers.map(p => (p.position, p.points))}"
        context.log.info(winMsg)
        context.log.debug(s"${winner.get}")
        playerRefs.values.foreach(_ ! GameOver(config.gameId, winMsg))
        Behaviors.stopped

      case StateMessage(state, MoveResponse(player, move, replyTo)) =>
        context.ask[GetMoveResultProviderMessage[GAME], ResultResponse](resultProvider)(ref => GetMoveResultProviderMessage(state, player, move, ref)) {
          case Success(r@ResultResponse(id, result)) =>
            respondSuccess(replyTo)
            StateMessage(state, r)
          case Success(_) =>
            respondFailure(replyTo)
            null
          case Failure(ex) => StateMessage(config.initState, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from last player's first initial placement and request for last player's second placement
      case StateMessage(state, ResultResponse(`lastPlayerId`, m@InitialPlacementMove(true, v, e))) =>
        context.log.debug(s"$lastPlayerId: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        gamestate = gamestate.apply(m).state

        sendMove(lastPlayerId, m)

        context.ask[RequestMessage[GAME], MoveResponse](playerRefs(lastPlayerId))(ref => InitialPlacementRequest(config.gameId, gamestate.toPublicGameState, gamestate.players.getPlayer(lastPlayerId).inventory, lastPlayerId, false, ref)) {
          case Success(r@MoveResponse(`lastPlayerId`, InitialPlacementMove(false, _, _), replyTo)) =>
            respondSuccess(replyTo)
            StateMessage(gamestate, r)
          case Success(MoveResponse(id, move, replyTo)) =>
            println(id, move)
            respondFailure(replyTo)
            null
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initState, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from first player's second initial placement and request for first player's turn
      case StateMessage(state, ResultResponse(`firstPlayerId`, m@InitialPlacementMove(false, v, e))) =>
        context.log.debug(s"$firstPlayerId: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        gamestate = state.apply(m).state

        sendMove(firstPlayerId, m)

        context.ask[RequestMessage[GAME], MoveResponse](playerRefs(firstPlayerId))(ref => MoveRequest(config.gameId, gamestate.toPublicGameState, gamestate.players.getPlayer(firstPlayerId).inventory, firstPlayerId, ref)) {
          case Success(r@MoveResponse(`firstPlayerId`, RollDiceMove, replyTo)) =>
            respondSuccess(replyTo)
            StateMessage(gamestate, r)
          case Success(MoveResponse(_, _, replyTo)) =>
            respondFailure(replyTo)
            null
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initState, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player's first initial placement and request for next players first initial placement
      case StateMessage(state, ResultResponse(id, m@InitialPlacementMove(true, v, e))) =>
        context.log.debug(s"$id: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        gamestate = state.apply(m).state

        sendMove(id, m)

        val nextId = gamestate.players.nextPlayer(id)

        context.ask[RequestMessage[GAME], MoveResponse](playerRefs(nextId))(ref => InitialPlacementRequest(config.gameId, gamestate.toPublicGameState, gamestate.players.getPlayer(nextId).inventory, nextId, true, ref)) {
          case Success(r@MoveResponse(`nextId`, InitialPlacementMove(true, _, _), replyTo)) =>
            respondSuccess(replyTo)
            StateMessage(gamestate, r)
          case Success(MoveResponse(_, _, replyTo)) =>
            respondFailure(replyTo)
            null
          case Success(_) => null
          case Failure(ex) => StateMessage(gamestate, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player's second initial placement and request for next players second initial placement
      case StateMessage(state, ResultResponse(id, m@InitialPlacementMove(false, v, e))) =>
        context.log.debug(s"$id: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        gamestate = state.apply(m).state

        sendMove(id, m)

        val prevId = gamestate.players.previousPlayer(id)

        context.ask[RequestMessage[GAME], MoveResponse](playerRefs(prevId))(ref => InitialPlacementRequest(config.gameId, gamestate.toPublicGameState, gamestate.players.getPlayer(prevId).inventory, prevId, false, ref)) {
          case Success(r@MoveResponse(`prevId`, InitialPlacementMove(false, _, _), replyTo)) =>
            respondSuccess(replyTo)
            StateMessage(gamestate, r)
          case Success(MoveResponse(_, _, replyTo)) =>
            respondFailure(replyTo)
            null
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initState, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player for rolling dice
      // If dice results are a 7 it will either ask for discards or ask to move robber
      // otherwise it will request a move
      case StateMessage(state, ResultResponse(id, m@RollResult(roll))) =>
        sendMove(id, m)

        context.log.debug(s"$id: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        gamestate = state.apply(m).state

        if (roll.number == 7) {
          val toDiscard = gamestate.players.getPlayers.filter(_.numCards > 7).map(_.position)
          if (!toDiscard.isEmpty) {
            discarding = Some(DiscardedCardsMapBuilder(toDiscard))
            toDiscard.foreach { _id =>
              context.ask[RequestMessage[GAME], MoveResponse](playerRefs(_id))(ref => DiscardCardRequest(config.gameId, gamestate.toPublicGameState, gamestate.players.getPlayer(_id).inventory, _id, ref)) {
                case Success(r@MoveResponse(`_id`, DiscardResourcesMove(res), replyTo)) =>
                  respondSuccess(replyTo)
                  StateMessage(gamestate, r)
                case Success(MoveResponse(_, _, replyTo)) =>
                  respondFailure(replyTo)
                  null
                case Success(_) => null
                case Failure(ex) => StateMessage(config.initState, ErrorMessage(ex))
              }
            }
          } else {
            context.ask[RequestMessage[GAME], MoveResponse](playerRefs(id))(ref => MoveRobberRequest(config.gameId, gamestate.toPublicGameState, gamestate.players.getPlayer(id).inventory, id, ref)) {
              case Success(r@MoveResponse(`id`, MoveRobberAndStealMove(_, _), replyTo)) =>
                respondSuccess(replyTo)
                StateMessage(gamestate, r)
              case Success(MoveResponse(_, _, replyTo)) =>
                respondFailure(replyTo)
                null
              case Success(_) => null
              case Failure(ex) => StateMessage(config.initState, ErrorMessage(ex))
            }
          }
          Behaviors.same
        } else turnMove(gamestate)

      case StateMessage(state, ResultResponse(id, m@DiscardResourcesResult(resLost))) =>
        sendMove(id, m)
        discarding = discarding.map(_.addDiscard(id, resLost(id)))
        if (!discarding.get.expectingDiscard) {
          val cardsLost = discarding.get.cardsToDiscard

          gamestate = state.playersDiscardFromSeven(cardsLost).state
          playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
          context.log.debug(s"$m")

          context.ask[RequestMessage[GAME], MoveResponse](playerRefs(id))(ref => MoveRobberRequest(config.gameId, gamestate.toPublicGameState, gamestate.players.getPlayer(id).inventory, id, ref)) {
            case Success(r@MoveResponse(`id`, MoveRobberAndStealMove(_, _), replyTo)) =>
              respondSuccess(replyTo)
              StateMessage(gamestate, r)
            case Success(MoveResponse(_, _, replyTo)) =>
              respondFailure(replyTo)
              null
            case Success(_) => null
            case Failure(ex) => StateMessage(gamestate, ErrorMessage(ex))
          }
        }
        Behaviors.same

      // Response from player after moving the robber and indicating who to steal from
      // then it requests a move
      case StateMessage(state, ResultResponse(id, result@MoveRobberAndStealResult(viewableBy, robberLocation, steal))) => turnMove {
        //        stealCards(states, id, robberLocation, victim)(MoveRobberUpdate)(MoveRobberAndStealResult) {
        //          case (state: GameState[PLAYERS], playerId: Int, robloc: Int, steal: Option[Steal]) => state.apply(playerId, MoveRobberAndStealResult(robloc, steal))
        //        }

        context.log.debug(s"$id: $result")
        sendMove(id, result)
        gamestate = state.apply(result).state

        playerRefs.foreach { case (refId, ref) =>
          if (viewableBy.contains(refId)) ref ! MoveResultUpdate(config.gameId, result)
          else steal match {
            case Some(RobPlayer(victim, Some(_))) =>
              ref ! MoveResultUpdate(config.gameId, MoveRobberAndStealResult(viewableBy, robberLocation, Some(RobPlayer(victim, None))))
            case Some(RobPlayer(_, None)) =>
              ref ! MoveResultUpdate(config.gameId, MoveRobberAndStealResult(viewableBy, robberLocation, None))
            case None =>
              ref ! MoveResultUpdate(config.gameId, MoveRobberAndStealResult(viewableBy, robberLocation, None))
          }
        }
        gamestate

        //        states.copy(
        //          states.apply(result),
        //          states.playerStates.filterNot(_._1 == id).map { case (playerId, state) =>
        //            steal match {
        //              case Some(RobPlayer(`playerId`, _))  =>
        //                playerRefs(playerId) ! MoveResultUpdate(config.gameId, result)
        //                playerId -> state.moveRobberAndSteal(robberLocation, steal)
        //              case Some(Steal(robber, victim, _)) =>
        //                playerRefs(playerId) ! MoveResultUpdate(config.gameId, MoveRobberAndStealResult(robberLocation, Some(Steal(robber, victim, None))))
        //                playerId -> state.moveRobberAndSteal(robberLocation, Some(Steal(robber, victim, None)))
        //              case None =>
        //                playerRefs(playerId) ! MoveResultUpdate(config.gameId, MoveRobberAndStealResult(robberLocation, None))
        //                playerId -> state.moveRobberAndSteal(robberLocation, None)
        //            }
        //          }
        //        )
      }

      case StateMessage(state, ResultResponse(id, m@BuyDevelopmentCardResult(viewableBy, nextCard))) => turnMove {
        context.log.debug(s"$id: $m")
        sendMove(id, m)

        gamestate = state.apply(m).state
        playerRefs.foreach { case (refId, ref) =>
          if (viewableBy.contains(refId)) ref ! MoveResultUpdate(config.gameId, m)
          else ref ! MoveResultUpdate(config.gameId, BuyDevelopmentCardResult(viewableBy, None))
        }
        gamestate
      }

      case StateMessage(state, ResultResponse(id, m@BuildRoadMove(edge))) => turnMove {
        def longest(gs: GameState[_]) = gs.players.getPlayer(id).roadPoints >= 2

        val hadLongest = longest(state)

        gamestate = state.apply(m).state

        sendMove(id, m)

        context.log.debug(s"$id $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))

        if (!hadLongest && longest(gamestate)) {
          context.log.debug(s"${LongestRoadUpdate(config.gameId, id)}")
          playerRefs.values.foreach(_ ! LongestRoadUpdate(config.gameId, id))
        }
        gamestate
      }

      case StateMessage(states, ResultResponse(id, m@BuildSettlementMove(vertex))) => turnMove {
        context.log.debug(s"$id: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        sendMove(id, m)
        states.apply(m).state
      }

      case StateMessage(states, ResultResponse(id, m@BuildCityMove(vertex))) => turnMove {
        context.log.debug(s"$id: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        sendMove(id, m)
        states.apply(m).state
      }

      case StateMessage(states, ResultResponse(id, m@PortTradeMove(give, get))) => turnMove {
        context.log.debug(s"$id $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        sendMove(id, m)
        states.apply(m).state
      }

      case StateMessage(state, ResultResponse(id, result@KnightResult(MoveRobberAndStealResult(viewableBy, robberLocation, steal)))) => turnMove {
        def largest(gs: GameState[_]) = gs.players.getPlayer(id).armyPoints >= 2

        val hadLargest = largest(state)

        context.log.debug(s"$id: $result")
        sendMove(id, result)
        gamestate = state.apply(result).state

        playerRefs.foreach { case (refId, ref) =>
          if (viewableBy.contains(refId)) ref ! MoveResultUpdate(config.gameId, result)
          else steal match {
            case Some(RobPlayer(victim, Some(_))) =>
              ref ! MoveResultUpdate(config.gameId, KnightResult(MoveRobberAndStealResult(viewableBy, robberLocation, Some(RobPlayer(victim, None)))))
            case Some(RobPlayer(_, None)) =>
              ref ! MoveResultUpdate(config.gameId, KnightResult(MoveRobberAndStealResult(viewableBy, robberLocation, None)))
            case None =>
              ref ! MoveResultUpdate(config.gameId, KnightResult(MoveRobberAndStealResult(viewableBy, robberLocation, None)))
          }
        }

        if (!hadLargest && largest(gamestate)) {
          context.log.debug(s"${LargestArmyUpdate(config.gameId, id)}")
          playerRefs.values.foreach(_ ! LargestArmyUpdate(config.gameId, id))
        }
        gamestate
      }

      case StateMessage(states, ResultResponse(id, m@YearOfPlentyMove(res1, res2))) => turnMove {
        context.log.debug(s"$id: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        sendMove(id, m)
        states.apply(m).state
      }

      case StateMessage(states, ResultResponse(id, m@MonopolyResult(cardsLost))) => turnMove {
        context.log.debug(s"$id: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))
        sendMove(id, m)
        states.apply(m).state
      }

      case StateMessage(state, ResultResponse(id, m@RoadBuilderMove(road1, road2))) => turnMove {
        def longest(gs: GameState[_]) = gs.players.getPlayer(id).roadPoints >= 2

        val hadLongest = longest(state)

        sendMove(id, m)

        gamestate = state.apply(m).state
        context.log.debug(s"$id: $m")
        playerRefs.values.foreach(_ ! MoveResultUpdate(config.gameId, m))

        if (!hadLongest && longest(gamestate)) {
          context.log.debug(s"${LongestRoadUpdate(config.gameId, id)}")
          playerRefs.values.foreach(_ ! LongestRoadUpdate(config.gameId, id))
        }
        gamestate
      }

      case StateMessage(state, ResultResponse(id, m@EndTurnMove)) => turnMove {
        context.log.debug(s"$id: $m")
        context.log.debug(s"${TurnUpdate(config.gameId, state.players.nextPlayer(id))}")

        sendMove(id, m)

        playerRefs.values.foreach { ref =>
          ref ! MoveResultUpdate(config.gameId, m)
          ref ! TurnUpdate(config.gameId, state.players.nextPlayer(id))
        }
        gamestate = state.apply(m).state
        gamestate
      }

      case StateMessage(states, ErrorMessage(ex)) =>
        context.log.error("errorMessage: {}", ex)
        Behaviors.stopped

      case null =>
        context.log.error(s"ERROR")
        Behaviors.stopped

      case _ => Behaviors.same
    }
  }

  def gameBehavior2[GAME <: Inventory[GAME], BOARD <: BoardConfiguration](config: GameConfiguration[GAME, BOARD]) = Behaviors.setup[Behavior2Messages] { context =>

    implicit val timeout: Timeout = 60.seconds
    implicit val scheduler = context.system.scheduler
    implicit val ec: ExecutionContextExecutor = context.executionContext

    val resultProvider = context.spawn[MoveResultProviderMessage[GAME]](MoveResultProvider.moveResultProvider(config.resultProvider), s"${config.gameId.key}_result_provider")

    def getNextAction: Unit = {
      config.context.getNextAction.foreach { action =>
        context.pipeToSelf(action) {
          case Success(r@ReceiveMoveFromClient(id, move)) if config.context.canDoMove(move) =>
            r.moveResponse.success(ProtoMoveResponse("SUCCESS"))
            ToMoveResult(move, config.context.getGameState, id)
          case Success(r@ReceiveMoveFromClient(_, _)) =>
            r.moveResponse.success(ProtoMoveResponse("FAILURE"))
            null
        }
      }
    }

    getNextAction

    Behaviors.receiveMessage[Behavior2Messages] {

      case ToMoveResult(move, state: GameState[GAME], id) =>
        context.ask[GetMoveResultProviderMessage[GAME], ResultResponse](resultProvider)(ref => GetMoveResultProviderMessage[GAME](state, id, move, ref)) {
          case Success(ResultResponse(_, result: DiscardResourcesResult)) =>
            context.log.debug(s"$id: $result")
            config.context.updateGameState(id, result)
            if (config.context.getGameState.phase != GamePhase.Discard) MoveRequest2
            else Empty

          case Success(ResultResponse(_, result: MoveResult)) =>
            context.log.debug(s"$id: $result")
            config.context.updateGameState(id, result)
            MoveRequest2
          case Failure(ex) => null
        }
        Behaviors.same

      case MoveRequest2 if config.context.getGameState.isOver =>
        val state = config.context.getGameState
        val winner = state.players.getPlayers.find(_.points >= 10)
        val winMsg = s"Player ${winner.get.position} has won game ${config.gameId} with ${winner.get.points} points and ${state.turn} rolls ${state.players.getPlayers.map(p => (p.position, p.points))}"
        context.log.info(winMsg)
        Behaviors.stopped

      case MoveRequest2 =>
        getNextAction
        Behaviors.same

      case Empty => Behaviors.same

      case other =>
        println(other)
        Behaviors.same

    }
  }

  sealed trait Behavior2Messages

  case class ToMoveResult[GAME <: Inventory[GAME]](catanMove: CatanMove, gameState: GameState[GAME], pos: Int) extends Behavior2Messages
  case object MoveRequest2 extends Behavior2Messages
  case object Empty extends Behavior2Messages

}

