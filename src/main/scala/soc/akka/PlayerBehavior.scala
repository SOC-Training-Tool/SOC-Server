package soc.akka

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import soc.akka.messages.RequestMessage._
import soc.akka.messages.UpdateMessage
import soc.akka.messages.UpdateMessage._
import soc.akka.messages.{GameMessage, MoveResponse, Terminate}
import soc.game.inventory.Inventory
import soc.playerRepository.PlayerContext

import scala.concurrent.ExecutionContext

object PlayerBehavior {

  def playerBehavior[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER], T <: Inventory[T]](playerContext: PlayerContext[GAME, T])(implicit ec: ExecutionContext): Behavior[GameMessage] = Behaviors.setup[GameMessage] { context =>

    Behaviors.receiveMessage {

      case StartGame(gameId, board, players) =>
        playerContext.addGameState(gameId, board, players)
        Behaviors.same
      
      case InitialPlacementUpdate(gameId, id, first, settlement, road) =>
        playerContext.updateGameState(gameId)(_.initialPlacement(id, first, settlement, road))
        Behaviors.same
      case EndTurnUpdate(gameId, id) =>
        playerContext.updateGameState(gameId)(_.endTurn(id))
        Behaviors.same
      case RollDiceUpdate(gameId, id, roll, _) =>
        playerContext.updateGameState(gameId)(_.rollDice(id, roll))
        Behaviors.same
      case DiscardCardsUpdate(gameId, cardsDiscarded) =>
        playerContext.updateGameState(gameId)(_.playersDiscardFromSeven(cardsDiscarded))
        Behaviors.same
      case MoveRobberUpdate(gameId, id, loc, steal) =>
        playerContext.updateGameState(gameId)(_.moveRobberAndSteal(id, loc, steal))
        Behaviors.same
      case BuildSettlementUpdate(gameId, id, settlement) =>
        playerContext.updateGameState(gameId)(_.buildSettlement(id, settlement))
        Behaviors.same
      case BuildCityUpdate(gameId, id, city) =>
        playerContext.updateGameState(gameId)(_.buildCity(id, city))
        Behaviors.same
      case BuildRoadUpdate(gameId, id, road) =>
        playerContext.updateGameState(gameId)(_.buildRoad(id, road))
        Behaviors.same
      case BuyDevelopmentCardUpdate(gameId, id, card) =>
        playerContext.updateGameState(gameId)(_.buyDevelopmentCard(id, card))
        Behaviors.same
      case PortTradeUpdate(gameId, id, give, get) =>
        playerContext.updateGameState(gameId)(_.portTrade(id, give, get))
        Behaviors.same
      case KnightUpdate(gameId, id, loc, steal) =>
        playerContext.updateGameState(gameId)(_.playKnight(id, loc, steal))
        Behaviors.same
      case MonopolyUpdate(gameId, id, resourcesMoved) =>
        playerContext.updateGameState(gameId)(_.playMonopoly(id, resourcesMoved))
        Behaviors.same
      case YearOfPlentyUpdate(gameId, id, res1, res2) =>
        playerContext.updateGameState(gameId)(_.playYearOfPlenty(id, res1, res2))
        Behaviors.same
      case RoadBuilderUpdate(gameId, id, road1, road2) =>
        playerContext.updateGameState(gameId)(_.playRoadBuilder(id, road1, road2))

        Behaviors.same
      case _: UpdateMessage =>
        Behaviors.same

      case request: InitialPlacementRequest[GAME, PLAYER] =>
        playerContext.moveSelector.initialPlacementMove(playerContext.getGameState(request.gameId), request.inventory, request.playerId)(request.first).map { move =>
          request.respondTo ! MoveResponse(request.playerId, move)
        }
        Behaviors.same

      case request: DiscardCardRequest[GAME, PLAYER] =>
        playerContext.moveSelector.discardCardsMove(playerContext.getGameState(request.gameId), request.inventory, request.playerId).map { move =>
          request.respondTo ! MoveResponse(request.playerId, move)
        }
        Behaviors.same

      case request: MoveRobberRequest[GAME, PLAYER] =>
        playerContext.moveSelector.moveRobberAndStealMove(playerContext.getGameState(request.gameId), request.inventory, request.playerId).map { move =>
          request.respondTo ! MoveResponse(request.playerId, move)
        }
        Behaviors.same

      case request: MoveRequest[GAME, PLAYER] =>
        playerContext.moveSelector.turnMove(playerContext.getGameState(request.gameId), request.inventory, request.playerId).map { move =>
          request.respondTo ! MoveResponse(request.playerId, move)
        }
        Behaviors.same

      case _ => Behaviors.stopped
    }
  }
}
