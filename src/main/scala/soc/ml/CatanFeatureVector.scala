package soc.ml

import soc.game.GameState
import soc.game.board.{BoardHex, CatanBoard}
import soc.game.inventory.Inventory.ProbableInfo
import soc.game.inventory.developmentCard.DevCardInventory
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory.{Brick, CatanPoint, Knight, Misc, Monopoly, Ore, Port, Resource, RoadBuilder, Sheep, Wheat, Wood, YearOfPlenty}
import soc.game.player.PlayerState

import scala.annotation.tailrec

object CatanFeatureVector {

  implicit def playerToVector(ps: PlayerState[ProbableInfo]): List[Double] = { //64 * 4
    ps.position.toDouble ::
      ps.numCards.toDouble ::
      ps.inventory.probableResourceSet.getAmount(Brick) ::
      ps.inventory.probableResourceSet.getAmount(Wood) ::
      ps.inventory.probableResourceSet.getAmount(Sheep) ::
      ps.inventory.probableResourceSet.getAmount(Wheat) ::
      ps.inventory.probableResourceSet.getAmount(Ore) ::
      ps.dots.getAmount(Brick).toDouble ::
      ps.dots.getAmount(Wood).toDouble ::
      ps.dots.getAmount(Sheep).toDouble ::
      ps.dots.getAmount(Wheat).toDouble ::
      ps.dots.getAmount(Ore).toDouble ::
      ps.points.toDouble ::
      ps.boardPoints.toDouble ::
      ps.armyPoints.toDouble ::
      ps.roadPoints.toDouble ::
      ps.dCardPoints.toDouble ::
      ps.inventory.playedDevCards.getTotal.toDouble ::
      ps.inventory.playedDevCards.getAmount(Knight).toDouble ::
      ps.inventory.playedDevCards.getAmount(Monopoly).toDouble ::
      ps.inventory.playedDevCards.getAmount(RoadBuilder).toDouble ::
      ps.inventory.playedDevCards.getAmount(YearOfPlenty).toDouble ::
      ps.inventory.playedDevCards.getAmount(CatanPoint).toDouble ::
      ps.inventory.knownUnplayedDevCards.getTotal.toDouble ::
      ps.inventory.knownUnplayedDevCards.getAmount(Knight).toDouble ::
      ps.inventory.knownUnplayedDevCards.getAmount(Monopoly).toDouble ::
      ps.inventory.knownUnplayedDevCards.getAmount(RoadBuilder).toDouble ::
      ps.inventory.knownUnplayedDevCards.getAmount(YearOfPlenty).toDouble ::
      ps.inventory.knownUnplayedDevCards.getAmount(CatanPoint).toDouble ::
      ps.inventory.probableDevCards.getTotal.toDouble ::
      ps.inventory.probableDevCards.getAmount(Knight).toDouble ::
      ps.inventory.probableDevCards.getAmount(Monopoly).toDouble ::
      ps.inventory.probableDevCards.getAmount(RoadBuilder).toDouble ::
      ps.inventory.probableDevCards.getAmount(YearOfPlenty).toDouble ::
      ps.inventory.probableDevCards.getAmount(CatanPoint).toDouble ::
      (if (ps.ports.contains(Brick)) 1.0 else 0.0) ::
      (if (ps.ports.contains(Ore)) 1.0 else 0.0) ::
      (if (ps.ports.contains(Sheep)) 1.0 else 0.0) ::
      (if (ps.ports.contains(Wheat)) 1.0 else 0.0) ::
      (if (ps.ports.contains(Wood)) 1.0 else 0.0) ::
      (if (ps.ports.contains(Misc)) 1.0 else 0.0) ::
      (ps.settlements.map(_.node.toDouble) ::: (1 to (5 - ps.settlements.size)).map(_ => 0.0).toList) :::
      (ps.cities.map(_.node.toDouble) ::: (1 to (4 - ps.cities.size)).map(_ => 0.0).toList) :::
      (ps.roads.map(_.toDouble) ::: (1 to (15 - ps.roads.size)).map(_ => 0.0).toList)
  }

  implicit def boardToVector(board: CatanBoard): List[Double] = { //53
    board.hexesWithNodes.sortBy(_.node).flatMap { node =>
      node.hex.getResourceAndNumber.fold(List(-1.0, -1.0)) {case (res, roll) =>
          List(res.id.toDouble, roll.number.toDouble)
      }
    }.toList :::
    board.portMap.toSeq.sortBy(_._1.node).map(_._2.id.toDouble).toList :::
    List(board.robberHex.toDouble) :::
    {
      val dots = board.hexesWithNodes
        .filter(_.hex.getNumber.isDefined)
        .groupBy(_.hex.getResource.get)
        .foldLeft(CatanResourceSet.empty[Int]){
          case (dots, (res, hexes)) => dots.add(hexes.map(_.hex.getNumber.get.dots).sum, res)
        }
      dots.getAmount(Brick).toDouble ::
      dots.getAmount(Wood).toDouble ::
      dots.getAmount(Sheep).toDouble ::
      dots.getAmount(Wheat).toDouble ::
      dots.getAmount(Ore).toDouble :: Nil
    }
  }

  implicit def gameStateToVector(gs: GameState[ProbableInfo]): List[Double] = { //324
    @tailrec
    def arrangePlayers(pos: Int, collector: List[PlayerState[ProbableInfo]] = Nil): List[PlayerState[ProbableInfo]] = {
      if (collector.contains(gs.players.getPlayer(pos))) collector
      else {
        arrangePlayers(gs.players.nextPlayer(pos), collector ::: List(gs.players.getPlayer(pos)))
      }
    }

    gs.diceRolls.toDouble ::
    gs.players.getPlayers.map(_.points).sum ::
    boardToVector(gs.board) ::: {
      val playedDCards = gs.players.getPlayers
        .map(_.inventory.playedDevCards)
        .foldLeft(DevCardInventory.empty[Int]) { case (total, set) => total.add(set) }
      (if (gs.turnState.canRollDice) 1.0 else 0.0) ::
      (if (gs.turnState.canPlayDevCard) 1.0 else 0.0) ::
      gs.bank.getAmount(Brick).toDouble ::
      gs.bank.getAmount(Wood).toDouble ::
      gs.bank.getAmount(Sheep).toDouble ::
      gs.bank.getAmount(Wheat).toDouble ::
      gs.bank.getAmount(Ore).toDouble ::
      gs.devCardsDeck.toDouble ::
      playedDCards.getAmount(Knight).toDouble ::
      playedDCards.getAmount(Monopoly).toDouble ::
      playedDCards.getAmount(RoadBuilder).toDouble ::
      playedDCards.getAmount(YearOfPlenty).toDouble ::
      playedDCards.getAmount(CatanPoint).toDouble :: Nil
    } :::
    arrangePlayers(gs.currPlayer).flatMap(playerToVector)
  }
}
