package soc.akka

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import soc.akka.messages.{ErrorMessage, GameMessage, RequestMessage, Response, StateMessage, UpdateMessage}
import soc.game.CatanMove._
import soc.game.{CatanMove, CatanPlayCardMove, DevelopmentCard, GameState, Resource, Roll}
import soc.game.board.{CatanBoard, Edge, Vertex}
import soc.game.dice.Dice
import soc.akka.messages.RequestMessage._
import soc.akka.messages.UpdateMessage._
import soc.akka.messages.{ErrorMessage, GameMessage, Response, StateMessage, UpdateMessage}
import soc.game.player.{NoInfoPlayerState, PerfectInfoPlayerState}
import soc.game.resources.{CatanResourceSet, DiscardedCardsMapBuilder, Steal}
import soc.sql.MoveEntry

import scala.concurrent.duration._
import scala.util.{Failure, Random, Success}

import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

case class GameStateHolder(gameState: GameState, playerStates: Map[Int, GameState]) {
  def update(stateUpdater: GameState => GameState): GameStateHolder = GameStateHolder (
    stateUpdater(gameState),
    playerStates.mapValues(stateUpdater)
  )
}

object GameBehavior {

  def gameBehavior(gameId: Int, board: CatanBoard, dice: Dice, dcardDeck: List[DevelopmentCard], players: Map[(String, Int), ActorRef[GameMessage]])(implicit random: Random) = Behaviors.setup[StateMessage] { context =>

    implicit val timeout: Timeout = 60.seconds

    val playerIds = players.keys.map(_._2).toSeq.sorted
    val playerRefs: Map[Int, ActorRef[GameMessage]] = players.map {
      case ((_, id), ref) => (id, ref)
    }

    val firstPlayerId = playerIds.min
    val lastPlayerId = playerIds.max

    def nextPlayer(playerId: Int): Int = {
      val indexOf = playerIds.indexOf(playerId)
      playerIds.drop(indexOf + 1).headOption.getOrElse(firstPlayerId)
    }

    def previousPlayer(playerId: Int): Int = {
      val indexOf = playerIds.indexOf(playerId)
      playerIds.dropRight(playerIds.length - indexOf).lastOption.getOrElse(lastPlayerId)
    }

    var discarding: Option[DiscardedCardsMapBuilder] = None

    val initStates = {
      val perfectInfoPlayers = players.keys.map { case (name, id) =>
        PerfectInfoPlayerState(name, id)
      }.toList

      def imperfectInfoPlayers(playerId: Int) = players.keys.map {
        case (name, `playerId`) => PerfectInfoPlayerState(name, playerId)
        case (name, id) => NoInfoPlayerState(name, id)
      }.toList

      GameStateHolder(GameState(board, perfectInfoPlayers), playerIds.map(p => p -> GameState(board, imperfectInfoPlayers(p))).toMap)
    }

    // Send first request for first player's initial placement
    context.ask[RequestMessage, Response](playerRefs(firstPlayerId))(ref => InitialPlacementRequest(initStates.playerStates(firstPlayerId), firstPlayerId, true, ref)) {
      case Success(r @ Response(`firstPlayerId`, InitialPlacementMove(true, _, _))) => StateMessage(initStates, r)
      case Success(_) => null
      case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
    }

    def turnMove(id: Int)(updateState: => GameStateHolder): Behavior[StateMessage] = {
      val states = updateState

      val winner = states.gameState.players.find(_.points >= 10)
      if (winner.isDefined) {
        context.log.info(s"Player ${winner.get.position} has won with ${winner.get.points} points and ${states.gameState.diceRolls} rolls ${states.gameState.players.map(p => (p.position, p.points))}")
        context.log.debug(s"${winner.get}")
        Behaviors.stopped
      }
      else {
        context.ask[RequestMessage, Response](playerRefs(id))(ref => MoveRequest(states.playerStates(id), id, ref)) {
          case Success(Response(`id`, RollDiceMove)) if !states.gameState.turnState.canRollDice => null
          case Success(Response(`id`, _: CatanPlayCardMove[_])) if !states.gameState.turnState.canPlayDevCard => null
          case Success(Response(`id`, EndTurnMove)) if states.gameState.turnState.canRollDice => null

          case Success(r @ Response(`id`, _: CatanMove[_])) => StateMessage(states, r)

          case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
        }
        Behaviors.same
      }
    }

    def stealCards(states: GameStateHolder, id: Int, robberLocation: Int, victim: Option[Int])
                  (updateMessage: (Int, Int, Option[Steal]) => UpdateMessage)
                  (stateUpdater: (GameState, Int, Int, Option[Steal]) => GameState): GameStateHolder = {
      val steal = victim.flatMap(player => states.gameState.getPlayer(player).getRandomCard.map((player, _))).map {
        case (victim, card) => Steal(id, victim, Some(CatanResourceSet(card)))
      }
      context.log.debug(s"${updateMessage(id, robberLocation, steal)}")

      states.copy(
        stateUpdater(states.gameState, id, robberLocation, steal),
        states.playerStates.map { case (playerId, state) =>
          steal match {
            case Some(Steal(robber, victim, _)) if robber == playerId || victim == playerId =>
              playerRefs(playerId) ! updateMessage(id, robberLocation, steal)
              playerId -> stateUpdater(state, id, robberLocation, steal)
            case Some(Steal(robber, victim, _)) =>
              playerRefs(playerId) ! updateMessage(id, robberLocation, Some(Steal(robber, victim, None)))
              playerId -> stateUpdater(state, id, robberLocation, Some(Steal(robber, victim, None)))
            case None =>
              playerRefs(playerId) ! updateMessage(id, robberLocation, None)
              playerId -> stateUpdater(state, id, robberLocation, None)
          }
        }
      )
    }

    Behaviors.receiveMessage {

      // Response from last player's first initial placement and request for last player's second placement
      case StateMessage(states, Response(`lastPlayerId`, m @ InitialPlacementMove(true, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(lastPlayerId, true, v, e)}")
        playerRefs.values.foreach(_ ! InitialPlacementUpdate(lastPlayerId, true, v, e))
        val newStates = states.update(_.apply(lastPlayerId, m))

        context.ask[RequestMessage, Response](playerRefs(lastPlayerId))(ref => InitialPlacementRequest(newStates.playerStates(lastPlayerId), lastPlayerId, false, ref)) {
          case Success(r @ Response(`lastPlayerId`, InitialPlacementMove(false, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from first player's second initial placement and request for first player's turn
      case StateMessage(states, Response(`firstPlayerId`, m @ InitialPlacementMove(false, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(firstPlayerId, false, v, e)}")
        playerRefs.values.foreach(_ ! InitialPlacementUpdate(firstPlayerId, false, v, e))
        val newStates = states.update(_.apply(firstPlayerId, m))

        context.ask[RequestMessage, Response](playerRefs(firstPlayerId))(ref => MoveRequest(newStates.playerStates(firstPlayerId), firstPlayerId, ref)) {
          case Success(r @ Response(`firstPlayerId`, RollDiceMove)) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player's first initial placement and request for next players first initial placement
      case StateMessage(states, Response(id, m @ InitialPlacementMove(true, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(id, true, v, e)}")
        playerRefs.values.foreach(_ ! InitialPlacementUpdate(id, true, v, e))
        val newStates = states.update(_.apply(id, m))

        val nextId = nextPlayer(id)

        context.ask[RequestMessage, Response](playerRefs(nextId))(ref => InitialPlacementRequest(newStates.playerStates(nextId), nextId, true, ref)) {
          case Success(r @ Response(`nextId`, InitialPlacementMove(true, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player's second initial placement and request for next players second initial placement
      case StateMessage(states, Response(id, m @ InitialPlacementMove(false, v, e))) =>
        context.log.debug(s"${InitialPlacementUpdate(id, false, v, e)}")
        playerRefs.values.foreach(_ ! InitialPlacementUpdate(id, false, v, e))
        val newStates = states.update(_.apply(id, m))

        val prevId = previousPlayer(id)

        context.ask[RequestMessage, Response](playerRefs(prevId))(ref => InitialPlacementRequest(newStates.playerStates(prevId), prevId, false, ref)) {
          case Success(r @ Response(`prevId`, InitialPlacementMove(false, _, _))) => StateMessage(newStates, r)
          case Success(_) => null
          case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
        }
        Behaviors.same

      // Response from player for rolling dice
      // If dice results are a 7 it will either ask for discards or ask to move robber
      // otherwise it will request a move
      case StateMessage(states, Response(id, m @ RollDiceMove)) =>
        val (roll1, roll2) = dice.getRoll
        val roll = Roll(roll1 + roll2)
        val resourcesGained = states.gameState.board.getResourcesGainedOnRoll(roll.number)
        context.log.debug(s"${RollDiceUpdate(id, roll, resourcesGained)}")
        playerRefs.values.foreach(_ ! RollDiceUpdate(id, roll, resourcesGained))
        val newStates = states.update(_.apply(id, RollResult(roll)))

        if (roll.number == 7) {
          val toDiscard = newStates.gameState.players.filter(_.numCards > 7).map(_.position)
          if (!toDiscard.isEmpty) {
            discarding = Some(DiscardedCardsMapBuilder(toDiscard))
            toDiscard.foreach { _id =>
              context.ask[RequestMessage, Response](playerRefs(_id))(ref => DiscardCardRequest(newStates.playerStates(_id), _id, ref)) {
                case Success(r @ Response(`_id`, DiscardResourcesMove(_))) => StateMessage(newStates, r)
                case Success(_) => null
                case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
              }
            }

          } else {
            context.ask[RequestMessage, Response](playerRefs(id))(ref => MoveRobberRequest(newStates.playerStates(id), id, ref)) {
              case Success(r @ Response(`id`, MoveRobberAndStealMove(_, _))) => StateMessage(newStates, r)
              case Success(_) => null
              case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
            }
          }
          Behaviors.same
        } else turnMove(id)(newStates)

      case StateMessage(states, Response(id, DiscardResourcesMove(res))) =>
        discarding = discarding.map(_.addDiscard(id, res))
        if (!discarding.get.expectingDiscard) {
          val cardsLost = discarding.get.cardsToDiscard
          val newStates = states.update(_.playersDiscardFromSeven(cardsLost))
          playerRefs.values.foreach(_ ! DiscardCardsUpdate(cardsLost))
          context.log.debug(s"${DiscardCardsUpdate(cardsLost)}")
          discarding = None

          context.ask[RequestMessage, Response](playerRefs(id))(ref => MoveRobberRequest(newStates.playerStates(id), id, ref)) {
            case Success(r @ Response(`id`, MoveRobberAndStealMove(_, _))) => StateMessage(newStates, r)
            case Success(_) => null
            case Failure(ex) => StateMessage(initStates, ErrorMessage(ex))
          }
        }
        Behaviors.same

      // Response from player after moving the robber and indicating who to steal from
      // then it requests a move
      case StateMessage(states, Response(id, MoveRobberAndStealMove(robberLocation, victim))) => turnMove(id) {
        stealCards(states, id, robberLocation, victim)(MoveRobberUpdate){
          case(state: GameState, playerId: Int, robloc: Int, steal: Option[Steal]) => state.apply(playerId, MoveRobberAndStealResult(robloc, steal))
        }
      }

      case StateMessage(states, Response(id, BuyDevelopmentCardMove)) => turnMove(id) {
        val nextCard = dcardDeck.drop(25 - states.gameState.devCardsDeck).headOption
        context.log.debug(s"${BuyDevelopmentCardUpdate(id, nextCard)}")

        val newGameState = states.gameState.apply(id, BuyDevelopmentCardResult(nextCard))
        val newPlayerStates = states.playerStates.map {
          case (`id`, state) =>
            playerRefs(id) ! BuyDevelopmentCardUpdate(id, nextCard)
            id -> state.apply(id, BuyDevelopmentCardResult(nextCard))
          case (playerId, state) =>
            playerRefs(playerId) ! BuyDevelopmentCardUpdate(id, None)
            playerId -> state.apply(id, BuyDevelopmentCardResult(None))
        }

        GameStateHolder(newGameState, newPlayerStates)
      }

      case StateMessage(states, Response(id, m @ BuildRoadMove(edge))) => turnMove(id) {
        def longest(gs: GameState) = gs.getPlayer(id).roadPoints >= 2
        val hadLongest = longest(states.gameState)

        val newStates = states.update(_.apply(id, m))

        context.log.debug(s"${BuildRoadUpdate(id, edge)}")
        playerRefs.values.foreach(_ ! BuildRoadUpdate(id, edge))

        if (!hadLongest && longest(newStates.gameState)) {
          context.log.debug(s"${LongestRoadUpdate(id)}")
          playerRefs.values.foreach(_ ! LongestRoadUpdate(id))
        }
        newStates
      }

      case StateMessage(states, Response(id, m @ BuildSettlementMove(vertex))) => turnMove(id) {
        context.log.debug(s"${BuildSettlementUpdate(id, vertex)}")
        playerRefs.values.foreach(_ ! BuildSettlementUpdate(id, vertex))
        states.update(_.apply(id, m))
      }

      case StateMessage(states, Response(id, m @ BuildCityMove(vertex))) => turnMove(id) {
        context.log.debug(s"${BuildCityUpdate(id, vertex)}")
        playerRefs.values.foreach(_ ! BuildCityUpdate(id, vertex))
        states.update(_.apply(id, m))
      }

      case StateMessage(states, Response(id, m @ PortTradeMove(give, get))) => turnMove(id) {
        context.log.debug(s"${PortTradeUpdate(id, give, get)}")
        playerRefs.values.foreach(_ ! PortTradeUpdate(id, give, get))
        states.update(_.apply(id, m))
      }

      case StateMessage(states, Response(id, KnightMove(MoveRobberAndStealMove(robberLocation, victim)))) => turnMove(id) {

        def largest(gs: GameState) = gs.getPlayer(id).armyPoints >= 2
        val hadLargest = largest(states.gameState)

        val newStates = stealCards(states, id, robberLocation, victim)(KnightUpdate){
          case(state: GameState, id: Int, robloc: Int, steal: Option[Steal]) => state.apply(id, KnightResult(MoveRobberAndStealResult(robloc, steal)))
        }
        if (!hadLargest && largest(newStates.gameState)) {
          context.log.debug(s"${ LargestArmyUpdate(id)}")
          playerRefs.values.foreach(_ ! LargestArmyUpdate(id))
        }
        newStates
      }

      case StateMessage(states, Response(id, m @ YearOfPlentyMove(res1, res2))) => turnMove(id) {
        context.log.debug(s"${ YearOfPlentyUpdate(id, res1, res2)}")
        playerRefs.values.foreach(_ ! YearOfPlentyUpdate(id, res1, res2))
        states.update(_.apply(id, m))
      }

      case StateMessage(states, Response(id, MonopolyMove(res))) => turnMove(id) {
        val cardsLost = states.gameState.players.map { p =>
          p.position -> CatanResourceSet.empty.add(p.getAmount(res).toInt, res)
        }.toMap
        context.log.debug(s"${MonopolyUpdate(id, cardsLost)}")
        playerRefs.values.foreach(_ ! MonopolyUpdate(id, cardsLost))
        states.update(_.apply(id, MonopolyResult(cardsLost)))
      }

      case StateMessage(states, Response(id, m @ RoadBuilderMove(road1, road2))) => turnMove(id) {
        def longest(gs: GameState) = gs.getPlayer(id).roadPoints >= 2
        val hadLongest = longest(states.gameState)

        val newStates = states.update(_.apply(id, m))
        context.log.debug(s"${RoadBuilderUpdate(id, road1, road2)}")
        playerRefs.values.foreach(_ ! RoadBuilderUpdate(id, road1, road2))

        if (!hadLongest && longest(newStates.gameState)) {
          context.log.debug(s"${LongestRoadUpdate(id)}")
          playerRefs.values.foreach(_ ! LongestRoadUpdate(id))
        }
        newStates
      }

      case StateMessage(states, Response(id, m @ EndTurnMove)) => turnMove(nextPlayer(id)) {
        context.log.debug(s"${EndTurnUpdate(id)}")
        context.log.debug(s"${TurnUpdate(nextPlayer(id))}")
        playerRefs.values.foreach { ref =>
          ref ! EndTurnUpdate(id)
          ref ! TurnUpdate(nextPlayer(id))
        }
        states.update(_.apply(id, m))
      }

      case StateMessage(initStates, ErrorMessage(ex)) =>
        context.log.error("errorMessage: {}\nstates: {}", ex, initStates)
        Behaviors.stopped

      case null =>
        context.log.error(s"ERROR")
        Behaviors.stopped
    }
  }
}
