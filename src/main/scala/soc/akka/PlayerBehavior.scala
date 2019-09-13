package soc.akka

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import soc.akka.messages.RequestMessage._
import soc.akka.messages.UpdateMessage
import soc.akka.messages.UpdateMessage._
import soc.akka.messages.{GameMessage, MoveResponse, Terminate}
import soc.game.{GameRules, GameState}
import soc.game.inventory.{Inventory, InventoryManagerFactory}
import soc.game.player.PlayerStateManager
import soc.game.player.moveSelector.MoveSelector

import scala.concurrent.ExecutionContext

object PlayerBehavior {

  def playerBehavior[GAME <: Inventory[GAME], PLAYER <: Inventory[PLAYER], T <: Inventory[T]](moveSelector: MoveSelector[GAME, T], playerId: String)(implicit ec: ExecutionContext, factory: InventoryManagerFactory[T], gameRules: GameRules): Behavior[GameMessage] = Behaviors.setup[GameMessage] { context =>

    var gameStates: Map[Int, GameState[T]] = Map.empty

    def handleMessage(x: GameMessage): Behavior[GameMessage] = {
        println(playerId + x.getClass().toString())
        x match {
        case StartGame(gameId, board, players) =>
          gameStates = gameStates + (gameId -> GameState(board, PlayerStateManager(players)))
          Behaviors.same
        
        case InitialPlacementUpdate(gameId, id, first, settlement, road) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.initialPlacement(id, first, settlement, road))
          Behaviors.same
        case EndTurnUpdate(gameId, id) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.endTurn(id))
          Behaviors.same
        case RollDiceUpdate(gameId, id, roll, _) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.rollDice(id, roll))
          Behaviors.same
        case DiscardCardsUpdate(gameId, cardsDiscarded) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.playersDiscardFromSeven(cardsDiscarded))
          Behaviors.same
        case MoveRobberUpdate(gameId, id, loc, steal) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.moveRobberAndSteal(id, loc, steal))
          Behaviors.same
        case BuildSettlementUpdate(gameId, id, settlement) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.buildSettlement(id, settlement))
          Behaviors.same
        case BuildCityUpdate(gameId, id, city) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.buildCity(id, city))
          Behaviors.same
        case BuildRoadUpdate(gameId, id, road) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.buildRoad(id, road))
          Behaviors.same
        case BuyDevelopmentCardUpdate(gameId, id, card) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.buyDevelopmentCard(id, card))
          Behaviors.same
        case PortTradeUpdate(gameId, id, give, get) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.portTrade(id, give, get))
          Behaviors.same
        case KnightUpdate(gameId, id, loc, steal) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.playKnight(id, loc, steal))
          Behaviors.same
        case MonopolyUpdate(gameId, id, resourcesMoved) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.playMonopoly(id, resourcesMoved))
          Behaviors.same
        case YearOfPlentyUpdate(gameId, id, res1, res2) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.playYearOfPlenty(id, res1, res2))
          Behaviors.same
        case RoadBuilderUpdate(gameId, id, road1, road2) =>
          val gs = gameStates(gameId)
          gameStates = (gameStates - gameId) + (gameId -> gs.playRoadBuilder(id, road1, road2))
          Behaviors.same
        case _: UpdateMessage =>
          Behaviors.same

        case request: InitialPlacementRequest[GAME, PLAYER] =>
          moveSelector.initialPlacementMove(gameStates(request.gameId), request.inventory, request.playerId)(request.first).map { move =>
            request.respondTo ! MoveResponse(request.playerId, move)
          }
          Behaviors.same

        case request: DiscardCardRequest[GAME, PLAYER] =>
          moveSelector.discardCardsMove(gameStates(request.gameId), request.inventory, request.playerId).map { move =>
            request.respondTo ! MoveResponse(request.playerId, move)
          }
          Behaviors.same

        case request: MoveRobberRequest[GAME, PLAYER] =>
          moveSelector.moveRobberAndStealMove(gameStates(request.gameId), request.inventory, request.playerId).map { move =>
            request.respondTo ! MoveResponse(request.playerId, move)
          }
          Behaviors.same

        case request: MoveRequest[GAME, PLAYER] =>
          moveSelector.turnMove(gameStates(request.gameId), request.inventory, request.playerId).map { move =>
            request.respondTo ! MoveResponse(request.playerId, move)
          }
          Behaviors.same

        case Terminate =>
          context.log.info("Terminating Player Actor")
          Behaviors.stopped

        case _ => Behaviors.stopped
      }
    }

    Behaviors.receiveMessage {
      case any => {
        handleMessage(any)
      }
    }

  }
}
