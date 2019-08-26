package soc.akka

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import soc.akka.messages.{ErrorMessage, MoveEntryMessage, ReceivedDiscard, RequestMessage, Response, StateMessage, UpdateMessage}
import soc.akka.messages.RequestMessage._
import soc.game.{CatanMove, CatanPlayCardMove, GameConfiguration, GameRules, GameState, MoveResult, Roll}
import soc.akka.messages.UpdateMessage._
import soc.sql.MoveEntry
import soc.game.CatanMove._
import soc.game.inventory.Inventory
import soc.game.inventory.resources.CatanResourceSet.Resources
import soc.game.inventory.resources.{CatanResourceSet, DiscardedCardsMapBuilder, Steal}

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

  def gameBehavior[GAME <: Inventory[GAME], PLAYERS <: Inventory[PLAYERS]](config: GameConfiguration[GAME, PLAYERS, _]) = Behaviors.setup[StateMessage[GAME, PLAYERS]] { context =>

    implicit val timeout: Timeout = 60.seconds
    implicit val ec: ExecutionContextExecutor = context.executionContext

    var discarding: Option[DiscardedCardsMapBuilder] = None
    var moveNumber = 0

    // Send first request for first player's initial placement

    val firstPlayerId = config.firstPlayerId
    val lastPlayerId = config.lastPlayerId

    config.playerRefs.values.foreach(ref => ref ! StartGame(config.gameId, config.initBoard, config.players.keys.toSeq))

    context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(firstPlayerId)) { ref =>
      InitialPlacementRequest(config.gameId,
        config.initStates.playerStates(firstPlayerId),
        config.initStates.gameState.players.getPlayer(firstPlayerId).inventory,
        firstPlayerId, true,
        ref)
    } {
      case Success(r@Response(`firstPlayerId`, InitialPlacementMove(true, _, _))) => StateMessage[GAME, PLAYERS](config.initStates, r)
      case Success(_) => null
      case Failure(ex) => StateMessage[GAME, PLAYERS](config.initStates, ErrorMessage(ex))
    }

    def turnMove(id: Int)(updateState: => GameStateHolder[GAME, PLAYERS]): Behavior[StateMessage[GAME, PLAYERS]] = {
      val states = updateState


      val winner = states.gameState.players.getPlayers.find(_.points >= 10)
      if (winner.isDefined) {
        context.log.info(s"Player ${winner.get.position} has won with ${winner.get.points} points and ${states.gameState.diceRolls} rolls ${states.gameState.players.getPlayers.map(p => (p.position, p.points))}")
        context.log.debug(s"${winner.get}")
        Behaviors.stopped
      }
      else {
        context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(id))(ref => MoveRequest(config.gameId, states.playerStates(id), states.gameState.players.getPlayer(id).inventory, id, ref)) {
          case Success(Response(`id`, RollDiceMove)) if !states.gameState.turnState.canRollDice => null
          case Success(Response(`id`, _: CatanPlayCardMove[_])) if !states.gameState.turnState.canPlayDevCard => null
          case Success(Response(`id`, EndTurnMove)) if states.gameState.turnState.canRollDice => null

          case Success(r@Response(`id`, _: CatanMove[_])) => StateMessage(states, r)

          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same
      }
    }

    def stealCards(states: GameStateHolder[GAME, PLAYERS], id: Int, robberLocation: Int, victim: Option[Int])
      (updateMessage: (Int, Int, Int, Option[Steal]) => UpdateMessage)
      (moveResult: (Int, Option[Steal]) => MoveResult[_])
      (stateUpdater: (GameState[PLAYERS], Int, Int, Option[Steal]) => GameState[PLAYERS]): GameStateHolder[GAME, PLAYERS] = {
      val stolenRes: Option[Resources] = config.resultProvider.stealCardMoveResult(states.gameState, victim).value.map(_.get).get.map(CatanResourceSet.empty[Int].add(1, _))
      val steal = victim.map(v => Steal(id, v, stolenRes))

      context.log.debug(s"${updateMessage(config.gameId, id, robberLocation, steal)}")
      val result = moveResult(robberLocation, steal)
      sendMove(id, result)

      states.copy(
        states.gameState(id, result),
        states.playerStates.map { case (playerId, state) =>
          steal match {
            case Some(Steal(robber, victim, _)) if robber == playerId || victim == playerId =>
              config.playerRefs(playerId) ! updateMessage(config.gameId, id, robberLocation, steal)
              playerId -> stateUpdater(state, id, robberLocation, steal)
            case Some(Steal(robber, victim, _)) =>
              config.playerRefs(playerId) ! updateMessage(config.gameId, id, robberLocation, Some(Steal(robber, victim, None)))
              playerId -> stateUpdater(state, id, robberLocation, Some(Steal(robber, victim, None)))
            case None =>
              config.playerRefs(playerId) ! updateMessage(config.gameId, id, robberLocation, None)
              playerId -> stateUpdater(state, id, robberLocation, None)
          }
        }
      )
    }

    def sendMove(id: Int, moveResult: MoveResult[_]): Unit = {
      config.moveRecorder.map(_ ! MoveEntryMessage(MoveEntry(config.gameId, moveNumber, config.playerIds(id), id, moveResult)))
      moveNumber = moveNumber + 1
    }

    Behaviors.receiveMessage {

      // Response from last player's first initial placement and request for last player's second placement
      case StateMessage(states, Response(`lastPlayerId`, m@InitialPlacementMove(true, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(config.gameId, lastPlayerId, true, v, e)}")
        config.playerRefs.values.foreach(_ ! InitialPlacementUpdate(config.gameId, lastPlayerId, true, v, e))
        val newStates = states.update(_.apply(lastPlayerId, m))

        sendMove(lastPlayerId, m)

        context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(lastPlayerId))(ref => InitialPlacementRequest(config.gameId, newStates.playerStates(lastPlayerId), newStates.gameState.players.getPlayer(lastPlayerId).inventory, lastPlayerId, false, ref)) {
          case Success(r@Response(`lastPlayerId`, InitialPlacementMove(false, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from first player's second initial placement and request for first player's turn
      case StateMessage(states, Response(`firstPlayerId`, m@InitialPlacementMove(false, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(config.gameId, firstPlayerId, false, v, e)}")
        config.playerRefs.values.foreach(_ ! InitialPlacementUpdate(config.gameId, firstPlayerId, false, v, e))
        val newStates = states.update(_.apply(firstPlayerId, m))

        sendMove(firstPlayerId, m)

        context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(firstPlayerId))(ref => MoveRequest(config.gameId, newStates.playerStates(firstPlayerId), newStates.gameState.players.getPlayer(firstPlayerId).inventory, firstPlayerId, ref)) {
          case Success(r@Response(`firstPlayerId`, RollDiceMove)) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player's first initial placement and request for next players first initial placement
      case StateMessage(states, Response(id, m@InitialPlacementMove(true, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(config.gameId, id, true, v, e)}")
        config.playerRefs.values.foreach(_ ! InitialPlacementUpdate(config.gameId, id, true, v, e))
        val newStates = states.update(_.apply(id, m))

        sendMove(id, m)

        val nextId = config.nextPlayer(id)

        context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(nextId))(ref => InitialPlacementRequest(config.gameId, newStates.playerStates(nextId), newStates.gameState.players.getPlayer(nextId).inventory, nextId, true, ref)) {
          case Success(r@Response(`nextId`, InitialPlacementMove(true, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player's second initial placement and request for next players second initial placement
      case StateMessage(states, Response(id, m@InitialPlacementMove(false, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(config.gameId, id, false, v, e)}")
        config.playerRefs.values.foreach(_ ! InitialPlacementUpdate(config.gameId, id, false, v, e))
        val newStates = states.update(_.apply(id, m))

        sendMove(id, m)

        val prevId = config.previousPlayer(id)

        context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(prevId))(ref => InitialPlacementRequest(config.gameId, newStates.playerStates(prevId), newStates.gameState.players.getPlayer(prevId).inventory, prevId, false, ref)) {
          case Success(r@Response(`prevId`, InitialPlacementMove(false, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player for rolling dice
      // If dice results are a 7 it will either ask for discards or ask to move robber
      // otherwise it will request a move
      case StateMessage(states, Response(id, m@RollDiceMove)) =>
        val roll = config.resultProvider.rollDiceMoveResult.value.map(_.get).get
        sendMove(id, RollResult(roll))

        val resourcesGained = states.gameState.board.getResourcesGainedOnRoll(roll.number)
        context.log.debug(s"${RollDiceUpdate(config.gameId, id, roll, resourcesGained)}")
        config.playerRefs.values.foreach(_ ! RollDiceUpdate(config.gameId, id, roll, resourcesGained))
        val newStates = states.update(_.apply(id, RollResult(roll)))

        if (roll.number == 7) {
          val toDiscard = newStates.gameState.players.getPlayers.filter(_.numCards > 7).map(_.position)
          if (!toDiscard.isEmpty) {
            discarding = Some(DiscardedCardsMapBuilder(toDiscard))
            toDiscard.foreach { _id =>
              context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(_id))(ref => DiscardCardRequest(config.gameId, newStates.playerStates(_id), newStates.gameState.players.getPlayer(_id).inventory, _id, ref)) {
                case Success(r @ Response(`_id`, DiscardResourcesMove(res))) => StateMessage(newStates, r)
                case Success(_) => null
                case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
              }
            }
          } else {
            context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(id))(ref => MoveRobberRequest(config.gameId, newStates.playerStates(id), newStates.gameState.players.getPlayer(id).inventory, id, ref)) {
              case Success(r@Response(`id`, MoveRobberAndStealMove(_, _))) => StateMessage(newStates, r)
              case Success(_) => null
              case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
            }
          }
          Behaviors.same
        } else turnMove(id)(newStates)

      case StateMessage(states, Response(id, m @ DiscardResourcesMove(res))) =>
        sendMove(id, m)
        discarding = discarding.map(_.addDiscard(id, res(id)))
        if (!discarding.get.expectingDiscard) {
          val cardsLost = discarding.get.cardsToDiscard

          val newStates = states.update(_.playersDiscardFromSeven(cardsLost))
          config.playerRefs.values.foreach(_ ! DiscardCardsUpdate(config.gameId, cardsLost))
          context.log.debug(s"${DiscardCardsUpdate(config.gameId, cardsLost)}")

          context.ask[RequestMessage[GAME, PLAYERS], Response](config.playerRefs(id))(ref => MoveRobberRequest(config.gameId, newStates.playerStates(id), newStates.gameState.players.getPlayer(id).inventory, id, ref)) {
            case Success(r@Response(`id`, MoveRobberAndStealMove(_, _))) => StateMessage(newStates, r)
            case Success(_) => null
            case Failure(ex) => StateMessage(config.initStates, ErrorMessage(ex))
          }
        }
        Behaviors.same

      // Response from player after moving the robber and indicating who to steal from
      // then it requests a move
      case StateMessage(states, Response(id, MoveRobberAndStealMove(robberLocation, victim))) => turnMove(id) {
        stealCards(states, id, robberLocation, victim)(MoveRobberUpdate)(MoveRobberAndStealResult) {
          case (state: GameState[PLAYERS], playerId: Int, robloc: Int, steal: Option[Steal]) => state.apply(playerId, MoveRobberAndStealResult(robloc, steal))
        }
      }

      case StateMessage(states, Response(id, BuyDevelopmentCardMove)) => turnMove(id) {
        val nextCard = config.resultProvider.buyDevelopmentCardMoveResult(states.gameState).value.map(_.get).get
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

      case StateMessage(states, Response(id, m@BuildRoadMove(edge))) => turnMove(id) {
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

      case StateMessage(states, Response(id, m
        @BuildSettlementMove(vertex)
      )) => turnMove(id) {
        context.log.debug(s"${BuildSettlementUpdate(config.gameId, id, vertex)}")
        config.playerRefs.values.foreach(_ ! BuildSettlementUpdate(config.gameId, id, vertex))
        sendMove(id, m)
        states.update(_.apply(id, m))
      }

      case StateMessage(states, Response(id, m
        @BuildCityMove(vertex)
      )) => turnMove(id) {
        context.log.debug(s"${BuildCityUpdate(config.gameId, id, vertex)}")
        config.playerRefs.values.foreach(_ ! BuildCityUpdate(config.gameId, id, vertex))
        sendMove(id, m)
        states.update(_.apply(id, m))
      }

      case StateMessage(states, Response(id, m
        @PortTradeMove(give, get)
      )) => turnMove(id) {
        context.log.debug(s"${PortTradeUpdate(config.gameId, id, give, get)}")
        config.playerRefs.values.foreach(_ ! PortTradeUpdate(config.gameId, id, give, get))
        sendMove(id, m)
        states.update(_.apply(id, m))
      }

      case StateMessage(states, Response(id, KnightMove(MoveRobberAndStealMove(robberLocation, victim)))) => turnMove(id) {
        def largest(gs: GameState[_]) = gs.players.getPlayer(id).armyPoints >= 2
        val hadLargest = largest(states.gameState)

        val newStates = stealCards(states, id, robberLocation, victim)(KnightUpdate) { case (a, b) => KnightResult(MoveRobberAndStealResult(a, b)) } {
          case (state: GameState[PLAYERS], id: Int, robloc: Int, steal: Option[Steal]) => state.apply(id, KnightResult(MoveRobberAndStealResult(robloc, steal)))
        }
        if (!hadLargest && largest(newStates.gameState)) {
          context.log.debug(s"${LargestArmyUpdate(config.gameId, id)}")
          config.playerRefs.values.foreach(_ ! LargestArmyUpdate(config.gameId, id))
        }
        newStates
      }

      case StateMessage(states, Response(id, m
        @YearOfPlentyMove(res1, res2)
      )) => turnMove(id) {
        context.log.debug(s"${YearOfPlentyUpdate(config.gameId, id, res1, res2)}")
        config.playerRefs.values.foreach(_ ! YearOfPlentyUpdate(config.gameId, id, res1, res2))
        sendMove(id, m)
        states.update(_.apply(id, m))
      }

      case StateMessage(states, Response(id, MonopolyMove(res))) => turnMove(id) {
        val cardsLost = config.resultProvider.playMonopolyMoveResult(states.gameState, res).value.map(_.get).get
        context.log.debug(s"${MonopolyUpdate(config.gameId, id, cardsLost)}")
        config.playerRefs.values.foreach(_ ! MonopolyUpdate(config.gameId, id, cardsLost))
        sendMove(id, MonopolyResult(cardsLost))
        states.update(_.apply(id, MonopolyResult(cardsLost)))
      }

      case StateMessage(states, Response(id, m@RoadBuilderMove(road1, road2))) => turnMove(id) {
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

      case StateMessage(states, Response(id, m
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
