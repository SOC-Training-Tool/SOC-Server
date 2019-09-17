package soc.akka

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import soc.akka.messages.RequestMessage._
import soc.akka.messages.{GameMessage, MoveResponse, PlayerDoMove, UpdateMessage}
import soc.akka.messages.UpdateMessage._
import soc.game.inventory.Inventory
import soc.playerRepository.PlayerContext

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object PlayerBehavior {

  def playerBehavior[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER]](playerContext: PlayerContext[GAME, _], position: Int)(implicit ec: ExecutionContext): Behavior[GameMessage] = Behaviors.setup[GameMessage] { context =>

      Behaviors.receiveMessage {

        case StartGame(gameId, board, players, respondTo) =>
          playerContext.addGameState(gameId, position, board, players)
          respondTo ! position
          Behaviors.same

        case InitialPlacementUpdate(gameId, id, first, settlement, road) =>
          playerContext.updateGameState(gameId, position)(_.initialPlacement(first, settlement, road))
          Behaviors.same
        case EndTurnUpdate(gameId, id) =>
          playerContext.updateGameState(gameId, position)(_.endTurn)
          Behaviors.same
        case RollDiceUpdate(gameId, id, roll, _) =>
          playerContext.updateGameState(gameId, position)(_.rollDice(roll))
          Behaviors.same
        case DiscardCardsUpdate(gameId, cardsDiscarded) =>
          playerContext.updateGameState(gameId, position)(_.playersDiscardFromSeven(cardsDiscarded))
          Behaviors.same
        case MoveRobberUpdate(gameId, id, loc, steal) =>
          playerContext.updateGameState(gameId, position)(_.moveRobberAndSteal(loc, steal))
          Behaviors.same
        case BuildSettlementUpdate(gameId, id, settlement) =>
          playerContext.updateGameState(gameId, position)(_.buildSettlement(settlement))
          Behaviors.same
        case BuildCityUpdate(gameId, id, city) =>
          playerContext.updateGameState(gameId, position)(_.buildCity(city))
          Behaviors.same
        case BuildRoadUpdate(gameId, id, road) =>
          playerContext.updateGameState(gameId, position)(_.buildRoad(road))
          Behaviors.same
        case BuyDevelopmentCardUpdate(gameId, id, card) =>
          playerContext.updateGameState(gameId, position)(_.buyDevelopmentCard(card))
          Behaviors.same
        case PortTradeUpdate(gameId, id, give, get) =>
          playerContext.updateGameState(gameId, position)(_.portTrade(give, get))
          Behaviors.same
        case KnightUpdate(gameId, id, loc, steal) =>
          playerContext.updateGameState(gameId, position)(_.playKnight(loc, steal))
          Behaviors.same
        case MonopolyUpdate(gameId, id, resourcesMoved) =>
          playerContext.updateGameState(gameId, position)(_.playMonopoly(resourcesMoved))
          Behaviors.same
        case YearOfPlentyUpdate(gameId, id, res1, res2) =>
          playerContext.updateGameState(gameId, position)(_.playYearOfPlenty(res1, res2))
          Behaviors.same
        case RoadBuilderUpdate(gameId, id, road1, road2) =>
          playerContext.updateGameState(gameId, position)(_.playRoadBuilder(road1, road2))

          Behaviors.same
        case _: UpdateMessage =>
          Behaviors.same

        case request: InitialPlacementRequest[GAME, PLAYER] =>
          context.pipeToSelf(playerContext.getInitialPlacementMove(request.gameId, position, request.inventory, request.playerId, request.first)) {
            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
            case Failure(ex) => null
          }
          Behaviors.same

        case request: DiscardCardRequest[GAME, PLAYER] =>
          context.pipeToSelf(playerContext.getDiscardCardMove(request.gameId, position, request.inventory, request.playerId)) {
            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
            case Failure(ex) => null
          }
          Behaviors.same

        case request: MoveRobberRequest[GAME, PLAYER] =>
          context.pipeToSelf(playerContext.getMoveRobberAndStealMove(request.gameId, position, request.inventory, request.playerId)) {
            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
            case Failure(ex) => null
          }
          Behaviors.same

        case request: MoveRequest[GAME, PLAYER] =>
          context.pipeToSelf(playerContext.getMove(request.gameId, position, request.inventory, request.playerId)) {
            case Success(move) => PlayerDoMove(request.playerId, move, request.respondTo)
            case Failure(ex) => null
          }
          Behaviors.same

        case PlayerDoMove(playerId, move, respondTo) =>
          respondTo ! MoveResponse(playerId, move)
          Behaviors.same


        case _ => Behaviors.stopped
    }
  }
}
