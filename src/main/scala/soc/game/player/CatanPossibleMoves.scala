package soc.game.player

import soc.game._
import soc.game.CatanMove._
import soc.game.board.{Edge, Vertex}
import soc.game.resources.CatanResourceSet
import soc.game.resources.CatanResourceSet.Resources

import scala.annotation.tailrec

case class CatanPossibleMoves (
  state: GameState,
  playerPosition: Int) {

  val currPlayer: PerfectInfoPlayerState = state.players.find(_.position == playerPosition).get.asInstanceOf[PerfectInfoPlayerState]
  val board = state.board
  val turnState = state.turnState
  val devCardsDeck = state.devCardsDeck

  def getPossibleMovesForState: List[CatanMove] = {

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

  def getPossibleBuilds: List[CatanBuildMove] = {
    //val player = getOurPlayerData
    val buildPotentialSettlements: List[CatanBuildMove] = if (currPlayer.canBuildSettlement) {
      board.getPossibleSettlements(currPlayer.position, false).toList.map(v => BuildSettlement(v))
    } else Nil

    val buildPotentialCities: List[CatanBuildMove] = if (currPlayer.canBuildCity) {
      board.getPossibleCities(currPlayer.position).toList.map(BuildCity)
    } else Nil

    val buildPotentialRoads: List[CatanBuildMove] = if (currPlayer.canBuildRoad) {
      board.getPossibleRoads(currPlayer.position).toList.map(BuildRoad)
    } else Nil

    val buildDevelopmentCard: List[CatanBuildMove] = if(currPlayer.canBuyDevCard && devCardsDeck > 0)  List(BuyDevelopmentCardMove)
    else Nil

    buildPotentialSettlements ::: buildPotentialCities ::: buildPotentialRoads ::: buildDevelopmentCard
  }

  def getPossiblePortTrades: List[PortTrade] = {
    val resourceSet = currPlayer.resourceSet

    val _3to1 = currPlayer.ports.contains(Misc)
    Resource.list.flatMap { res =>
      val otherRes = Resource.list.filterNot(_ == res)
      val num = resourceSet.getAmount(res)
      if ( currPlayer.ports.contains(res.asInstanceOf[Port]) && num >= 2) {
        val give = CatanResourceSet().add(2, res)
        otherRes.map{ r =>
          val get = CatanResourceSet().add(1, r)
          PortTrade(give,  get)
        }
      }
      else if (_3to1 && num >= 3) {
        val give =  CatanResourceSet().add(3, res)
        otherRes.map{ r =>
          val get = CatanResourceSet().add(1, r)
          PortTrade(give,  get)
        }
      }
      else if(num >= 4) {
        val give =  CatanResourceSet().add(4, res)
        otherRes.map{ r =>
          val get = CatanResourceSet().add(1, r)
          PortTrade(give,  get)
        }
      }
      else Nil
    }
  }

  def getPossibleRobberLocations: List[MoveRobberAndStealMove] = {
    board.hexesWithNodes
      .filterNot(_.node == board.robberHex)
      .flatMap { hex =>
        board.playersOnHex(hex.node).filterNot(p => p == currPlayer.position || state.getPlayer(p).numCards <= 0) match {
          case Nil => List(MoveRobberAndStealMove(hex.node, None))
          case list => list.map(n => MoveRobberAndStealMove(hex.node, Some(n)))
        }
      }
  }.toList

  final def getPossibleDiscards(numToDiscard: Int = state.getPlayer(playerPosition).numCards / 2) = {
    CatanSet.toList(currPlayer.resourceSet).combinations(numToDiscard).map { resList =>
      DiscardResourcesMove(CatanResourceSet(resList: _*))
    }
  }
//    stack: List[(Resources, Resources)] = List((CatanResourceSet(), currPlayer.resourceSet)),
//    discards: List[DiscardResourcesMove] = Nil): List[DiscardResourcesMove] = {
//
//    if (stack.isEmpty) discards.distinct
//    else {
//      val (toDiscard, ourRes) = stack.head
//      if (toDiscard.getTotal == numToDiscard) getPossibleDiscards(numToDiscard, stack.tail, DiscardResourcesMove(toDiscard) :: discards)
//      else {
//        val newStack = Resource.list.filter(ourRes.contains).map { res => (toDiscard.add(1, res), ourRes.subtract(1, res)) } ::: stack.tail
//        getPossibleDiscards(numToDiscard, newStack, discards)
//      }
//    }
//  }

  def getPossibleDevelopmentCard: List[CatanPlayCardMove] = {

    val knight: List[CatanPlayCardMove] = if (currPlayer.canPlayDevCards.contains(Knight)) {
      getPossibleRobberLocations.map(KnightMove)
    } else Nil

    val monopoly: List[CatanPlayCardMove] = if (currPlayer.canPlayDevCards.contains(Monopoly)) {
      Resource.list.map(MonopolyMove)
    } else Nil

    val yearOfPlenty: List[CatanPlayCardMove] = if (currPlayer.canPlayDevCards.contains(YearOfPlenty)) {
      Resource.list.flatMap { res1 =>
        Resource.list.map { res2 =>
          YearOfPlentyMove(res1, res2)
        }
      }
    } else Nil

    val roads: List[CatanPlayCardMove] = if (currPlayer.canPlayDevCards.contains(RoadBuilder) && currPlayer.roads.length < 15) {
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
