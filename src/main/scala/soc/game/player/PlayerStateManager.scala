package soc.game.player

import soc.game.GameRules
import soc.game.board.{CatanBoard, Edge, Vertex}
import soc.game.inventory.Inventory
import soc.game.inventory._
import soc.game.inventory.resources.SOCTransactions

case class PlayerStateManager [T <: Inventory[T]] protected (val players: Map[Int, PlayerState[T]])(
  implicit val inventoryManager: InventoryManager[T],
  implicit val gameRules: GameRules)  {

  def getPlayers: Seq[PlayerState[T]] = players.values.toList
  def getPlayer(id: Int): PlayerState[T] = players(id)

  def buildSettlement(id: Int, vertex: Vertex, board: CatanBoard): PlayerStateManager[T] = copy (
    players = players.map {
      case(`id`, ps) => id -> ps.buildSettlement(board, vertex)
      case (i, ps) => i -> ps
    }
  )
  def buildCity(id: Int, vertex: Vertex, board: CatanBoard): PlayerStateManager[T] = copy (
    players = players.map {
      case(`id`, ps) => id -> ps.buildCity(board, vertex)
      case (i, ps) => i -> ps
    }
  )

   def buildRoad(id: Int, edge: Edge, board: CatanBoard): PlayerStateManager[T] = copy (
    players = players.map {
      case(`id`, ps) => id -> ps.buildRoad(board, edge)
      case (i, ps) => i -> ps
    }
  )

  def playKnight(id: Int): PlayerStateManager[T] = playDevelopmentCard(id, Knight).updateLargestArmy

  def updateResources(transactions: List[SOCTransactions]): PlayerStateManager[T] = {
    val (newPlayers, newInvManager) = inventoryManager.updateResources(players, transactions)
    copy(newPlayers)(newInvManager, gameRules)
  }

  def buyDevelopmentCard(id: Int, card: Option[DevelopmentCard]):  PlayerStateManager[T] = {
    val (newPlayers, newInvManager) = inventoryManager.buyDevelopmentCard(players, id, card)
    copy(newPlayers)(newInvManager, gameRules)
  }

  def endTurn(id: Int): PlayerStateManager[T] = copy (
    players = players.map {
      case (`id`, ps) => id -> ps.endTurn
      case (i, ps) => i -> ps
    }
  )

  protected def playDevelopmentCard(id: Int, card: DevelopmentCard): PlayerStateManager[T] = {
    val (newPlayers, newInvManager) = inventoryManager.playDevelopmentCard(players, id, card)
    copy(newPlayers)(newInvManager, gameRules)
  }

  def playMonopoly(id: Int): PlayerStateManager[T] = playDevelopmentCard(id, Monopoly)
  def playYearOfPlenty(id: Int): PlayerStateManager[T] = playDevelopmentCard(id, YearOfPlenty)
  def playRoadBuilder(id: Int): PlayerStateManager[T] = playDevelopmentCard(id, RoadBuilder)

  def updateLongestRoad: PlayerStateManager[T] = {
    val longestRoad: Option[PlayerState[T]] = players.values.find(_.roadPoints >= 2)
    val newLongestRoad = players.values.filter(p => p.roadLength > longestRoad.fold(4)(_.roadLength)).headOption.map(_.position)
    copy (
      (longestRoad.map(_.position), newLongestRoad) match {
        case (_, None) => players
        case (None, Some(has)) =>  players.map {
          case (`has`, ps) => has -> ps.gainLongestRoad
          case (id, ps) => id -> ps
        }
        case (Some(had), Some(has)) => players.map {
          case (`had`, ps) => had -> ps.loseLongestRoad
          case (`has`, ps) => has -> ps.gainLongestRoad
          case (id, ps) => id -> ps
        }
      }
    )
  }

  def updateLargestArmy: PlayerStateManager[T] = {
    val largestArmy: Option[PlayerState[T]] = players.values.find(_.armyPoints >= 2)
    val newLargestArmy = players.values.filter{ p =>
      p.inventory.playedDevCards.getAmount(Knight) > largestArmy.fold(2)(_.inventory.playedDevCards.getAmount(Knight))
    }.headOption.map(_.position)
    copy (
      (largestArmy.map(_.position), newLargestArmy) match {
        case (_, None) => players
        case (None, Some(has)) =>  players.map {
          case (`has`, ps) => has -> ps.gainLargestArmy
          case (id, ps) => id -> ps
        }
        case (Some(had), Some(has)) => players.map {
          case (`had`, ps) => had -> ps.loseLargestArmy
          case (`has`, ps) => has -> ps.gainLargestArmy
          case (id, ps) => id -> ps
        }
      }
    )
  }
}

object PlayerStateManager {

  def apply[T <: soc.game.inventory.Inventory[T]](s: Seq[(String, Int)])(implicit factory: InventoryManagerFactory[T], gameRules: GameRules) = {
    implicit val invManager = factory.createInventoryManager
    new PlayerStateManager[T](s.map {case(name, id) => id -> PlayerState(name, id, invManager.createInventory(id)) }.toMap)
  }

}
