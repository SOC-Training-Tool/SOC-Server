package protocoder.implicits

import protos.soc.state.{Player, PublicGameState, PublicPlayerState => PPState, TurnPhase => PPhase}
import soc.inventory.{Brick, Inventory, Knight, Ore, PublicInfoInventoryHelper, Resource, Sheep, Wheat, Wood}
import soc.state.player.{PlayerState, PlayerStateHelper}
import soc.inventory.developmentCard.DevelopmentCardSet._
import protocoder.implicits.ResourceProto._
import protocoder.ProtoCoder
import protocoder.ProtoCoder.ops._
import soc.core.GameRules
import soc.inventory.Inventory.PublicInfo
import soc.inventory.resources.CatanResourceSet
import soc.state.{GameState, GamePhase}
import BoardProto._
import soc.board.BaseCatanBoard.baseBoardMapping
import util.MapReverse

object StateProto {

  private lazy val phaseMap: Map[GamePhase, PPhase] = Map(GamePhase.Roll -> PPhase.ROLL, GamePhase.BuyTradeOrEnd -> PPhase.BUY_TRADE_OR_END, GamePhase.Discard -> PPhase.DISCARD, GamePhase.MoveRobber -> PPhase.MOVE_ROBBER, GamePhase.InitialPlacement -> PPhase.INITIAL)
  private lazy val reversePhaseMap = MapReverse.reverseMap(phaseMap)

  implicit def protoPublicPlayerState[T <: Inventory[T]]: ProtoCoder[PlayerStateHelper[T], Map[Int, PPState]] = { ps =>
    ps.players.map { case (position, player) =>
      position -> PPState(
        Player(player.name, position),
        protoPublicInventory.proto(player.inventory.toPublicInfo),
        player.roadLength,
        player.playedDevCards.getAmount(Knight),
        player.armyPoints == 2,
        player.roadPoints == 2
      )
    }
  }

  implicit val protoGameState: ProtoCoder[GameState[PublicInfo], PublicGameState] = { gs =>
    val currP = gs.currentPlayer
    PublicGameState(
      gs.board.proto,
      gs.players.proto,
      gs.resourceBank.proto,
      gs.developmentCardsLeft,
      Player(gs.players.getPlayer(currP).name, currP),
      phaseMap(gs.phase),
      gs.turn
    )
  }

  implicit val publicGameStateFromProto: ProtoCoder[PublicGameState, GameState[PublicInfo]] = { gs =>

    // Todo embed and then extract game rules into PublicGameState
    implicit val gameRules = GameRules.default

    implicit val invHelper = PublicInfoInventoryHelper()
    val board = gs.board.proto
    val helper = {
      val playerStates = gs.playerStates.map[Int, PlayerState[PublicInfo]] { case pos -> ps =>
        val publicInfo: PublicInfo = ps.publicInventory.proto

        pos -> PlayerState[PublicInfo](
          ps.player.name,
          ps.player.position,
          ps.publicInventory.proto,
          if (ps.hasLargest) 2 else 0,
          if (ps.hasLongest) 2 else 0,
          board.getPortsForPlayer(pos),
          board.getSettlementVerticesForPlayer(pos).toList,
          board.getNumCityVerticesForPlayer(pos).toList,
          board.getRoadEdgesForPlayer(pos).toList,
          CatanResourceSet.empty[Int],
          board.longestRoadLength(pos)
        )
      }
      PlayerStateHelper(playerStates)
    }
    GameState(
      board,
      helper,
      gs.resourceBank.proto,
      gs.developmentCardsLeft,
      gs.currentTurnPlayer.position,
      reversePhaseMap(gs.phase),
      gs.turn,
      gameRules,
      Nil)
  }

}
