package soc.akka

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import soc.akka.MoveResultProviderMessage.GetMoveResultProviderMessage
import soc.akka.messages.{ErrorMessage, MoveEntryMessage, MoveResponse, ReceivedDiscard, RequestMessage, ResultResponse, SaveGameMessage, StateMessage, UpdateMessage}
import soc.akka.messages.RequestMessage.{InitialPlacementRequest, _}
import soc.game.{CatanMove, CatanPlayCardMove, GameConfiguration, GameRules, GameState, MoveResult, Roll}
import soc.akka.messages.UpdateMessage._
import soc.game._
import soc.game.board.BoardConfiguration
import soc.game.inventory.Inventory
import soc.game.inventory.resources.{DiscardedCardsMapBuilder, Steal}
import soc.storage

import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContextExecutor

case class GameStateHolder[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS]](
  gameState: GameState[GAME],
  playerStates: Map[Int, GameState[PLAYERS]]
) {

  def update(f: GameState[_] => GameState[_]) = copy(
    f(gameState).asInstanceOf[GameState[GAME]],
    playerStates.mapValues(s => f(s).asInstanceOf[GameState[PLAYERS]])
  )

}

object GameBehavior {

  def gameBehavior[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS], BOARD <: BoardConfiguration](config: GameConfiguration[GAME, PLAYERS, BOARD]) = Behaviors.setup[StateMessage[GAME, PLAYERS]] { context =>

    implicit val timeout: Timeout = 60.seconds
    implicit val ec: ExecutionContextExecutor = context.executionContext

    var discarding: Option[DiscardedCardsMapBuilder] = None
    var moveNumber = 0

    // Send first request for first player's initial placement

    val firstPlayerId = config.firstPlayerId
    val lastPlayerId = config.lastPlayerId

    config.playerRefs.values.foreach(ref => ref ! StartGame(config.gameId, config.initBoard, config.players.keys.toSeq))

    context.log.info(s"Starting Game ${config.gameId}")

    context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(firstPlayerId)) { ref =>
      InitialPlacementRequest(config.gameId,
        config.initStates.playerStates(firstPlayerId),
        config.initStates.gameState.players.getPlayer(firstPlayerId).inventory,
        firstPlayerId, true,
        ref)
    } {
      case Success(r@MoveResponse(`firstPlayerId`, InitialPlacementMove(true, _, _))) => StateMessage[GAME, PLAYERS](config.initStates, r)
      case Success(_) => null
      case Failure(ex) => StateMessage[GAME, PLAYERS](config.initStates, ErrorMessage(ex))
    }

    def turnMove(id: Int)(updateState: => GameStateHolder[GAME, PLAYERS]): Behavior[StateMessage[GAME, PLAYERS]] = {
      val states = updateState

      val winner = states.gameState.players.getPlayers.find(_.points >= 10)
      if (winner.isDefined) {
        config.moveRecorder.map(_ ! SaveGameMessage(config.gameId, config.boardConfig, states.gameState.players.getPlayers.map(p => (p.name, p.position) -> p.points).toMap))
        context.log.info(s"Player ${winner.get.position} has won with ${winner.get.points} points and ${states.gameState.diceRolls} rolls ${states.gameState.players.getPlayers.map(p => (p.position, p.points))}")
        context.log.debug(s"${winner.get}")
        Behaviors.stopped
      }
      else {
        context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(id))(ref => MoveRequest(config.gameId, states.playerStates(id), states.gameState.players.getPlayer(id).inventory, id, ref)) {
          case Success(MoveResponse(`id`, RollDiceMove)) if !states.gameState.turnState.canRollDice => null
          case Success(MoveResponse(`id`, _: CatanPlayCardMove)) if !states.gameState.turnState.canPlayDevCard => null
          case Success(MoveResponse(`id`, EndTurnMove)) if states.gameState.turnState.canRollDice => null

          case Success(r@MoveResponse(`id`, _: CatanMove)) => StateMessage(states, r)

          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same
      }
    }

    def sendMove(id: Int, moveResult: MoveResult): Unit = {
      config.moveRecorder.map(_ ! MoveEntryMessage(storage.MoveEntry(config.gameId, moveNumber, config.playerIds(id), id, moveResult)))
      moveNumber = moveNumber + 1
    }

    Behaviors.receiveMessage {

      case StateMessage(states, MoveResponse(player, move)) =>
        context.ask[GetMoveResultProviderMessage[GAME], ResultResponse](config.resultProvider)(ref => GetMoveResultProviderMessage(states.gameState, player, move, ref)) {
          case Success(r@ResultResponse(id, result)) => StateMessage(states, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from last player's first initial placement and request for last player's second placement
      case StateMessage(states, ResultResponse(`lastPlayerId`, m@InitialPlacementMove(true, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(config.gameId, lastPlayerId, true, v, e)}")
        config.playerRefs.values.foreach(_ ! InitialPlacementUpdate(config.gameId, lastPlayerId, true, v, e))
        val newStates = states.update(_.apply(lastPlayerId, m))

        sendMove(lastPlayerId, m)

        context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(lastPlayerId))(ref => InitialPlacementRequest(config.gameId, newStates.playerStates(lastPlayerId), newStates.gameState.players.getPlayer(lastPlayerId).inventory, lastPlayerId, false, ref)) {
          case Success(r@MoveResponse(`lastPlayerId`, InitialPlacementMove(false, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from first player's second initial placement and request for first player's turn
      case StateMessage(states, ResultResponse(`firstPlayerId`, m@InitialPlacementMove(false, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(config.gameId, firstPlayerId, false, v, e)}")
        config.playerRefs.values.foreach(_ ! InitialPlacementUpdate(config.gameId, firstPlayerId, false, v, e))
        val newStates = states.update(_.apply(firstPlayerId, m))

        sendMove(firstPlayerId, m)

        context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(firstPlayerId))(ref => MoveRequest(config.gameId, newStates.playerStates(firstPlayerId), newStates.gameState.players.getPlayer(firstPlayerId).inventory, firstPlayerId, ref)) {
          case Success(r@MoveResponse(`firstPlayerId`, RollDiceMove)) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player's first initial placement and request for next players first initial placement
      case StateMessage(states, ResultResponse(id, m@InitialPlacementMove(true, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(config.gameId, id, true, v, e)}")
        config.playerRefs.values.foreach(_ ! InitialPlacementUpdate(config.gameId, id, true, v, e))
        val newStates = states.update(_.apply(id, m))

        sendMove(id, m)

        val nextId = config.nextPlayer(id)

        context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(nextId))(ref => InitialPlacementRequest(config.gameId, newStates.playerStates(nextId), newStates.gameState.players.getPlayer(nextId).inventory, nextId, true, ref)) {
          case Success(r@MoveResponse(`nextId`, InitialPlacementMove(true, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player's second initial placement and request for next players second initial placement
      case StateMessage(states, ResultResponse(id, m@InitialPlacementMove(false, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(config.gameId, id, false, v, e)}")
        config.playerRefs.values.foreach(_ ! InitialPlacementUpdate(config.gameId, id, false, v, e))
        val newStates = states.update(_.apply(id, m))

        sendMove(id, m)

        val prevId = config.previousPlayer(id)

        context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(prevId))(ref => InitialPlacementRequest(config.gameId, newStates.playerStates(prevId), newStates.gameState.players.getPlayer(prevId).inventory, prevId, false, ref)) {
          case Success(r@MoveResponse(`prevId`, InitialPlacementMove(false, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player for rolling dice
      // If dice results are a 7 it will either ask for discards or ask to move robber
      // otherwise it will request a move
      case StateMessage(states, ResultResponse(id, m@RollResult(roll))) =>
        sendMove(id, m)

        val resourcesGained = states.gameState.board.getResourcesGainedOnRoll(roll.number)
        context.log.debug(s"${RollDiceUpdate(config.gameId, id, roll, resourcesGained)}")
        config.playerRefs.values.foreach(_ ! RollDiceUpdate(config.gameId, id, roll, resourcesGained))
        val newStates = states.update(_.apply(id, RollResult(roll)))

        if (roll.number == 7) {
          val toDiscard = newStates.gameState.players.getPlayers.filter(_.numCards > 7).map(_.position)
          if (!toDiscard.isEmpty) {
            discarding = Some(DiscardedCardsMapBuilder(toDiscard))
            toDiscard.foreach { _id =>
              context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(_id))(ref => DiscardCardRequest(config.gameId, newStates.playerStates(_id), newStates.gameState.players.getPlayer(_id).inventory, _id, ref)) {
                case Success(r @ MoveResponse(`_id`, DiscardResourcesMove(res))) => StateMessage(newStates, r)
                case Success(_) => null
                case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
              }
            }
          } else {
            context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(id))(ref => MoveRobberRequest(config.gameId, newStates.playerStates(id), newStates.gameState.players.getPlayer(id).inventory, id, ref)) {
              case Success(r@MoveResponse(`id`, MoveRobberAndStealMove(_, _))) => StateMessage(newStates, r)
              case Success(_) => null
              case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
            }
          }
          Behaviors.same
        } else turnMove(id)(newStates)

      case StateMessage(states, ResultResponse(id, m @ DiscardResourcesMove(res))) =>
        sendMove(id, m)
        discarding = discarding.map(_.addDiscard(id, res(id)))
        if (!discarding.get.expectingDiscard) {
          val cardsLost = discarding.get.cardsToDiscard

          val newStates = states.update(_.playersDiscardFromSeven(cardsLost))
          config.playerRefs.values.foreach(_ ! DiscardCardsUpdate(config.gameId, cardsLost))
          context.log.debug(s"${DiscardCardsUpdate(config.gameId, cardsLost)}")

          context.ask[RequestMessage[GAME, PLAYERS], MoveResponse](config.playerRefs(id))(ref => MoveRobberRequest(config.gameId, newStates.playerStates(id), newStates.gameState.players.getPlayer(id).inventory, id, ref)) {
            case Success(r@MoveResponse(`id`, MoveRobberAndStealMove(_, _))) => StateMessage(newStates, r)
            case Success(_) => null
            case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
          }
        }
        Behaviors.same

      // Response from player after moving the robber and indicating who to steal from
      // then it requests a move
      case StateMessage(states, ResultResponse(id, result@MoveRobberAndStealResult(robberLocation, steal))) => turnMove(id) {
//        stealCards(states, id, robberLocation, victim)(MoveRobberUpdate)(MoveRobberAndStealResult) {
//          case (state: GameState[PLAYERS], playerId: Int, robloc: Int, steal: Option[Steal]) => state.apply(playerId, MoveRobberAndStealResult(robloc, steal))
//        }

        context.log.debug(s"${MoveRobberUpdate(config.gameId, id, robberLocation, steal)}")
        sendMove(id, result)

        states.copy(
          states.gameState(id, result),
          states.playerStates.map { case (playerId, state) =>
            steal match {
              case Some(Steal(robber, victim, _)) if robber == playerId || victim == playerId =>
                config.playerRefs(playerId) ! MoveRobberUpdate(config.gameId, id, robberLocation, steal)
                playerId -> state.moveRobberAndSteal(id, robberLocation, steal)
              case Some(Steal(robber, victim, _)) =>
                config.playerRefs(playerId) ! MoveRobberUpdate(config.gameId, id, robberLocation, Some(Steal(robber, victim, None)))
                playerId -> state.moveRobberAndSteal(id, robberLocation, Some(Steal(robber, victim, None)))
              case None =>
                config.playerRefs(playerId) ! MoveRobberUpdate(config.gameId, id, robberLocation, None)
                playerId -> state.moveRobberAndSteal(id, robberLocation, None)
            }
          }
        )


      }

      case StateMessage(states, ResultResponse(id, BuyDevelopmentCardResult(nextCard))) => turnMove(id) {
        context.log.debug(s"${BuyDevelopmentCardUpdate(config.gameId, id, nextCard)}")

        val newGameState = states.gameState.apply(id, BuyDevelopmentCardResult(nextCard))
        val newPlayerStates = states.playerStates.map {
          case (`id`, state) =>
            config.playerRefs(id) ! BuyDevelopmentCardUpdate(config.gameId, id, nextCard)
            id -> state.apply(id, BuyDevelopmentCardResult(nextCard))
          case (playerId, state) =>
            config.playerRefs(playerId) ! BuyDevelopmentCardUpdate(config.gameId, id, None)
            playerId -> state.apply(id, BuyDevelopmentCardResult(None))
        }

        sendMove(id, BuyDevelopmentCardResult(nextCard))

        GameStateHolder(newGameState, newPlayerStates)
      }

      case StateMessage(states, ResultResponse(id, m@BuildRoadMove(edge))) => turnMove(id) {
        def longest(gs: GameState[_]) = gs.players.getPlayer(id).roadPoints >= 2

        val hadLongest = longest(states.gameState)

        val newStates = states.update(_.apply(id, m))

        sendMove(id, m)

        context.log.debug(s"${BuildRoadUpdate(config.gameId, id, edge)}")
        config.playerRefs.values.foreach(_ ! BuildRoadUpdate(config.gameId, id, edge))

        if (!hadLongest && longest(newStates.gameState)) {
          context.log.debug(s"${LongestRoadUpdate(config.gameId, id)}")
          config.playerRefs.values.foreach(_ ! LongestRoadUpdate(config.gameId, id))
        }
        newStates
      }

      case StateMessage(states, ResultResponse(id, m
        @BuildSettlementMove(vertex)
      )) => turnMove(id) {
        context.log.debug(s"${BuildSettlementUpdate(config.gameId, id, vertex)}")
        config.playerRefs.values.foreach(_ ! BuildSettlementUpdate(config.gameId, id, vertex))
        sendMove(id, m)
        states.update(_.apply(id, m))
      }

      case StateMessage(states, ResultResponse(id, m
        @BuildCityMove(vertex)
      )) => turnMove(id) {
        context.log.debug(s"${BuildCityUpdate(config.gameId, id, vertex)}")
        config.playerRefs.values.foreach(_ ! BuildCityUpdate(config.gameId, id, vertex))
        sendMove(id, m)
        states.update(_.apply(id, m))
      }

      case StateMessage(states, ResultResponse(id, m
        @PortTradeMove(give, get)
      )) => turnMove(id) {
        context.log.debug(s"${PortTradeUpdate(config.gameId, id, give, get)}")
        config.playerRefs.values.foreach(_ ! PortTradeUpdate(config.gameId, id, give, get))
        sendMove(id, m)
        states.update(_.apply(id, m))
      }

      case StateMessage(states, ResultResponse(id, result@KnightResult(MoveRobberAndStealResult(robberLocation, steal)))) => turnMove(id) {
        def largest(gs: GameState[_]) = gs.players.getPlayer(id).armyPoints >= 2
        val hadLargest = largest(states.gameState)

        val newStates = {
          context.log.debug(s"${KnightUpdate(config.gameId, id, robberLocation, steal)}")
          sendMove(id, result)

          states.copy(
            states.gameState(id, result),
            states.playerStates.map { case (playerId, state) =>
              steal match {
                case Some(Steal(robber, victim, _)) if robber == playerId || victim == playerId =>
                  config.playerRefs(playerId) ! KnightUpdate(config.gameId, id, robberLocation, steal)
                  playerId -> state.playKnight(id, robberLocation, steal)
                case Some(Steal(robber, victim, _)) =>
                  config.playerRefs(playerId) ! KnightUpdate(config.gameId, id, robberLocation, Some(Steal(robber, victim, None)))
                  playerId -> state.playKnight(id, robberLocation, Some(Steal(robber, victim, None)))
                case None =>
                  config.playerRefs(playerId) ! KnightUpdate(config.gameId, id, robberLocation, None)
                  playerId ->  state.playKnight(id, robberLocation, None)
              }
            }
          )
        }
        if (!hadLargest && largest(newStates.gameState)) {
          context.log.debug(s"${LargestArmyUpdate(config.gameId, id)}")
          config.playerRefs.values.foreach(_ ! LargestArmyUpdate(config.gameId, id))
        }
        newStates
      }

      case StateMessage(states, ResultResponse(id, m
        @YearOfPlentyMove(res1, res2)
      )) => turnMove(id) {
        context.log.debug(s"${YearOfPlentyUpdate(config.gameId, id, res1, res2)}")
        config.playerRefs.values.foreach(_ ! YearOfPlentyUpdate(config.gameId, id, res1, res2))
        sendMove(id, m)
        states.update(_.apply(id, m))
      }

      case StateMessage(states, ResultResponse(id, MonopolyResult(cardsLost))) => turnMove(id) {
        context.log.debug(s"${MonopolyUpdate(config.gameId, id, cardsLost)}")
        config.playerRefs.values.foreach(_ ! MonopolyUpdate(config.gameId, id, cardsLost))
        sendMove(id, MonopolyResult(cardsLost))
        states.update(_.apply(id, MonopolyResult(cardsLost)))
      }

      case StateMessage(states, ResultResponse(id, m@RoadBuilderMove(road1, road2))) => turnMove(id) {
        def longest(gs: GameState[_]) = gs.players.getPlayer(id).roadPoints >= 2

        val hadLongest = longest(states.gameState)

        sendMove(id, m)

        val newStates = states.update(_.apply(id, m))
        context.log.debug(s"${RoadBuilderUpdate(config.gameId, id, road1, road2)}")
        config.playerRefs.values.foreach(_ ! RoadBuilderUpdate(config.gameId, id, road1, road2))

        if (!hadLongest && longest(newStates.gameState)) {
          context.log.debug(s"${LongestRoadUpdate(config.gameId, id)}")
          config.playerRefs.values.foreach(_ ! LongestRoadUpdate(config.gameId, id))
        }
        newStates
      }

      case StateMessage(states, ResultResponse(id, m
        @EndTurnMove
      )) => turnMove(config.nextPlayer(id)) {
        context.log.debug(s"${EndTurnUpdate(config.gameId, id)}")
        context.log.debug(s"${TurnUpdate(config.gameId, config.nextPlayer(id))}")

        sendMove(id, m)

        config.playerRefs.values.foreach { ref =>
          ref ! EndTurnUpdate(config.gameId, id)
          ref ! TurnUpdate(config.gameId, config.nextPlayer(id))
        }
        states.update(_.apply(id, m))
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
}
