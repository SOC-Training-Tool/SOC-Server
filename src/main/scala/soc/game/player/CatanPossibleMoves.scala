package soc.game.player

import soc.game._
import soc.game.CatanMove._
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.inventory._
import soc.game.inventory.resources.CatanResourceSet

case class CatanPossibleMoves[PLAYER <: Inventory[PLAYER]] (state: GameState[PLAYER], inventory: PerfectInfo, playerPosition: Int) {

  val currPlayer = state.players.getPlayer(playerPosition)
  val board = state.board
  val turnState = state.turnState
  val devCardsDeck = state.devCardsDeck

  def getPossibleMovesForState: List[CatanMove.Move] = {

    val devCardMoves = if (turnState.canPlayDevCard) {
      getPossibleDevelopmentCard
    } else Nil

    val beforeOrAfterDiceMoves = if (turnState.canRollDice) List(RollDiceMove)
    else {
      EndTurnMove ::
        getPossibleBuilds :::
        getPossiblePortTrades
      //getPossibleTrades(state.player, state.game)
    }

    devCardMoves ::: beforeOrAfterDiceMoves
  }

  def getPossibleInitialPlacements(first: Boolean): Seq[InitialPlacementMove] = {
    board.getPossibleSettlements(currPlayer.position, true).flatMap { v =>
      val settlementBoard = board.buildSettlement(v, currPlayer.position)
      settlementBoard.edgesFromVertex(v).filter(settlementBoard.canBuildRoad(_, currPlayer.position)).map { e =>
        InitialPlacementMove(first, v, e)
      }
    }
  }

  lazy val getPossibleSettlements: List[CatanBuildMove[BuildSettlement]] = if (currPlayer.canBuildSettlement && inventory.canBuild(Settlement.cost)) {
    board.getPossibleSettlements(currPlayer.position, false).toList.map(v => BuildSettlementMove(v))
  } else Nil

  lazy val getPossibleCities: List[CatanBuildMove[BuildCity]] = if (currPlayer.canBuildCity && inventory.canBuild(City.cost)) {
    board.getPossibleCities(currPlayer.position).toList.map(BuildCityMove)
  } else Nil

  lazy val getPossibleRoads: List[CatanBuildMove[BuildRoad]] = if (currPlayer.canBuildRoad && inventory.canBuild(Road.cost)) {
    board.getPossibleRoads(currPlayer.position).toList.map(BuildRoadMove)
  } else Nil

  lazy val getPossibleDevelopmentCards: List[BuyDevelopmentCardMove.type] = if(currPlayer.canBuyDevelopmentCard && inventory.canBuild(DevelopmentCard.cost) && devCardsDeck > 0) {
    List(BuyDevelopmentCardMove)
  } else Nil


  def getPossibleBuilds: List[CatanBuildMove[_]] = {
   getPossibleSettlements ::: getPossibleCities ::: getPossibleRoads ::: getPossibleDevelopmentCards
  }

  def getPossiblePortTrades: List[PortTradeMove] = {
    val resourceSet = inventory.resourceSet

    val _3to1 = currPlayer.ports.contains(Misc)
    Resource.list.flatMap { res =>
      val otherRes = Resource.list.filterNot(_ == res)
      val num = resourceSet.getAmount(res)
      if ( currPlayer.ports.contains(res.asInstanceOf[Port]) && num >= 2) {
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

  def getPossibleRobberLocations: List[MoveRobberAndStealMove] = {
    board.hexesWithNodes
      .filterNot(_.node == board.robberHex)
      .flatMap { hex =>
        board.playersOnHex(hex.node).filterNot(p => p == currPlayer.position || state.players.getPlayer(p).numCards <= 0) match {
          case Nil => List(MoveRobberAndStealMove(hex.node, None))
          case list => list.map(n => MoveRobberAndStealMove(hex.node, Some(n)))
        }
      }
  }.toList
    def getPossibleDiscards(numToDiscard: Int = state.players.getPlayer(playerPosition).numCards / 2) = {
    CatanSet.toList(inventory.resourceSet).combinations(numToDiscard).map { resList =>
      DiscardResourcesMove(Map(playerPosition -> CatanResourceSet(resList: _*)))
    }
  }

  def getPossibleDevelopmentCard: List[CatanPlayCardMove[_]] = {

    val knight: List[CatanPlayCardMove[Knight]] = if (inventory.canPlayDevCards.contains(Knight)) {
      getPossibleRobberLocations.map(KnightMove)
    } else Nil

    val monopoly: List[CatanPlayCardMove[Monopoly]] = if (inventory.canPlayDevCards.contains(Monopoly)) {
      Resource.list.map(MonopolyMove)
    } else Nil

    val yearOfPlenty: List[CatanPlayCardMove[YearOfPlenty]] = if (inventory.canPlayDevCards.contains(YearOfPlenty)) {
      Resource.list.flatMap { res1 =>
        Resource.list.map { res2 =>
          YearOfPlentyMove(res1, res2)
        }
      }
    } else Nil

    val roads: List[CatanPlayCardMove[RoadBuilder]] = if (inventory.canPlayDevCards.contains(RoadBuilder) && currPlayer.roads.length < 15) {
      val firsRoadsAndBoards = board.getPossibleRoads(currPlayer.position).map { road1 =>
        (road1, board.buildRoad(road1, currPlayer.position))
      }
      if (currPlayer.roads.length < 14) {
        firsRoadsAndBoards.flatMap {case (road1, newBoard) =>
          newBoard.getPossibleRoads(currPlayer.position).map { road2 =>
            RoadBuilderMove(road1, Some(road2))
          }
        }
      } else firsRoadsAndBoards.map { case (edge, _) => RoadBuilderMove(edge, None)}
    }.toList
    else Nil

    knight ::: {
      if (turnState.canRollDice) Nil
      else monopoly ::: yearOfPlenty ::: roads
    }
  }
}
