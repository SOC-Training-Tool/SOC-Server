package soc.game.moves

import soc.game.GameState
import soc.game.board.CatanBoard
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory._
import soc.game.inventory.resources.CatanResourceSet.Resources
import soc.game.player.PlayerState

case class CatanPossibleMoves[U <: Inventory[U]](state: GameState[U], inventory: PerfectInfo, playerPosition: Int) {

  def getPossibleMovesForState = CatanPossibleMoves.getPossibleMovesForState(state, inventory, playerPosition)

  def getPossibleInitialPlacements(first: Boolean) = CatanPossibleMoves.getPossibleInitialPlacements(playerPosition, state.board, first)

  def getPossibleDiscards = CatanPossibleMoves.getPossibleDiscards(playerPosition, inventory.resourceSet)

  def getPossibleRobberLocations = CatanPossibleMoves.getPossibleRobberLocations(state, playerPosition)
}

object CatanPossibleMoves{

  def getPossibleMovesForState(state: GameState[_], inventory: PerfectInfo, playerPosition: Int): List[CatanMove] = {

    val devCardMoves = if (state.turnState.canPlayDevCard) {
      getPossibleDevelopmentCard(state, playerPosition, inventory)
    } else Nil

    val beforeOrAfterDiceMoves = if (state.turnState.canRollDice) List(RollDiceMove)
    else {
      EndTurnMove ::
        getPossibleBuilds(state.players.getPlayer(playerPosition), inventory, state.board, state.devCardsDeck) :::
        getPossiblePortTrades(inventory, state.players.getPlayer(playerPosition).ports)
      //getPossibleTrades(state.player, state.game)
    }

    devCardMoves ::: beforeOrAfterDiceMoves
  }

  def getPossibleInitialPlacements(position: Int, board: CatanBoard, first: Boolean): Seq[InitialPlacementMove] = {
    board.getPossibleSettlements(position, true).flatMap { v =>
      val settlementBoard = board.buildSettlement(v, position)
      settlementBoard.edgesFromVertex(v).filter(settlementBoard.canBuildRoad(_, position)).map { e =>
        InitialPlacementMove(first, v, e)
      }
    }
  }

  def getPossibleSettlements(currPlayer: PlayerState[_], inventory: PerfectInfo, board: CatanBoard): List[BuildSettlementMove] = if (currPlayer.canBuildSettlement && inventory.canBuild(Settlement.cost)) {
    board.getPossibleSettlements(currPlayer.position, false).toList.map(v => BuildSettlementMove(v))
  } else Nil

  def getPossibleCities(currPlayer: PlayerState[_], inventory: PerfectInfo, board: CatanBoard): List[BuildCityMove] = if (currPlayer.canBuildCity && inventory.canBuild(City.cost)) {
    board.getPossibleCities(currPlayer.position).toList.map(BuildCityMove)
  } else Nil

  def getPossibleRoads(currPlayer: PlayerState[_], inventory: PerfectInfo, board: CatanBoard): List[BuildRoadMove] = if (currPlayer.canBuildRoad && inventory.canBuild(Road.cost)) {
    board.getPossibleRoads(currPlayer.position).toList.map(BuildRoadMove)
  } else Nil

  def getPossibleDevelopmentCards(currPlayer: PlayerState[_], inventory: PerfectInfo, cardsLeftInDeck: Int): List[BuyDevelopmentCardMove.type] = if(currPlayer.canBuyDevelopmentCard && inventory.canBuild(DevelopmentCard.cost) && cardsLeftInDeck > 0) {
    List(BuyDevelopmentCardMove)
  } else Nil


  def getPossibleBuilds(currPlayer: PlayerState[_], inventory: PerfectInfo, board: CatanBoard, cardsLeftInDeck: Int): List[CatanBuildMove] = {
   getPossibleSettlements(currPlayer, inventory, board) :::
     getPossibleCities(currPlayer, inventory, board) :::
     getPossibleRoads(currPlayer, inventory, board) :::
     getPossibleDevelopmentCards(currPlayer, inventory, cardsLeftInDeck)
  }

  def getPossiblePortTrades(inventory: PerfectInfo, ports: Set[Port]): List[PortTradeMove] = {
    val resourceSet = inventory.resourceSet

    val _3to1 = ports.contains(Misc)
    Resource.list.flatMap { res =>
      val otherRes = Resource.list.filterNot(_ == res)
      val num = resourceSet.getAmount(res)
      if (ports.contains(res.asInstanceOf[Port]) && num >= 2) {
        val give = CatanResourceSet().add(2, res)
        otherRes.map{ r =>
          val get = CatanResourceSet().add(1, r)
          PortTradeMove(give,  get)
        }
      }
      else if (_3to1 && num >= 3) {
        val give =  CatanResourceSet().add(3, res)
        otherRes.map{ r =>
          val get = CatanResourceSet().add(1, r)
          PortTradeMove(give,  get)
        }
      }
      else if(num >= 4) {
        val give =  CatanResourceSet().add(4, res)
        otherRes.map{ r =>
          val get = CatanResourceSet().add(1, r)
          PortTradeMove(give,  get)
        }
      }
      else Nil
    }
  }

  def getPossibleRobberLocations(state: GameState[_], position: Int): List[MoveRobberAndStealMove] = {
    state.board.hexesWithNodes
      .filterNot(_.node == state.board.robberHex)
      .flatMap { hex =>
        state.board.playersOnHex(hex.node).filterNot(p => p == position || state.players.getPlayer(p).numCards <= 0) match {
          case Nil => List(MoveRobberAndStealMove(hex.node, None))
          case list => list.map(n => MoveRobberAndStealMove(hex.node, Some(n)))
        }
      }
  }.toList

  def getPossibleDiscards(playerPosition: Int, resourceSet: Resources) = {
    CatanSet.toList(resourceSet).combinations(resourceSet.getTotal / 2).map { resList =>
      DiscardResourcesMove(Map(playerPosition -> CatanResourceSet(resList: _*)))
    }
  }

  def getPossibleDevelopmentCard(state: GameState[_], position: Int, inventory: PerfectInfo): List[CatanPlayCardMove] = {
    val currPlayer = state.players.getPlayer(position)
    val board = state.board

    val knight: List[KnightMove] = if (inventory.canPlayDevCards.contains(Knight)) {
      getPossibleRobberLocations(state, position).map(KnightMove)
    } else Nil

    val monopoly: List[MonopolyMove] = if (inventory.canPlayDevCards.contains(Monopoly)) {
      Resource.list.map(MonopolyMove)
    } else Nil

    val yearOfPlenty: List[YearOfPlentyMove] = if (inventory.canPlayDevCards.contains(YearOfPlenty)) {
      Resource.list.flatMap { res1 =>
        Resource.list.map { res2 =>
          YearOfPlentyMove(res1, res2)
        }
      }
    } else Nil

    val roads: List[RoadBuilderMove] = if (inventory.canPlayDevCards.contains(RoadBuilder) && currPlayer.roads.length < 15) {
      val firsRoadsAndBoards = board.getPossibleRoads(position).map { road1 =>
        (road1, board.buildRoad(road1, position))
      }
      if (currPlayer.roads.length < 14) {
        firsRoadsAndBoards.flatMap {case (road1, newBoard) =>
          newBoard.getPossibleRoads(position).map { road2 =>
            RoadBuilderMove(road1, Some(road2))
          }
        }
      } else firsRoadsAndBoards.map { case (edge, _) => RoadBuilderMove(edge, None)}
    }.toList
    else Nil

    knight ::: {
      if (state.turnState.canRollDice) Nil
      else monopoly ::: yearOfPlenty ::: roads
    }
  }
}
