package soc.game

import log.Log
import soc.game.CatanMove.{BuildCity, BuildRoad, BuildSettlement, DiscardResourcesMove, EndTurnMove, InitialPlacementMove, PortTrade, RoadBuilderMove, RollDiceMove, YearOfPlentyMove}
import soc.game.DevCardInventory.PlayedInventory
import soc.game.board.{CatanBoard, Edge, Vertex}
import soc.game.player.{CatanPossibleMoves, PlayerState}
import soc.game.resources.CatanResourceSet._
import soc.game.resources.{CatanResourceSet, Gain, Lose, PossibleHands, SOCPossibleHands, SOCTransactions, Steal}

import scala.annotation.tailrec

case class GameState(board: CatanBoard,
                     players: List[PlayerState],
                     //possibleHands: PossibleHands = SOCPossibleHands.empty,
                     turnState: TurnState = TurnState(),
                     bank: Resources = CatanResourceSet.fullBank,
                     devCardsDeck: Int = 25,
                     transactions: List[SOCTransactions] = Nil,
                     diceRolls: Int = 0
                   ) {

  def getPlayer(playerId: Int) = players.find(_.position == playerId).get

  val turn = diceRolls / 4
//
//  def getStateArray: List[Double] = {
//    val probableResourceSets = possibleHands.toProbableResourceSets
//
//    List(board.getStateArray, bank.getStateArray, List(devCardsDeck.toDouble)).flatten :::
//      players.flatMap(p => p.getStateArray(probableResourceSets(p.position)))
//  }


  val gameOver = players.exists(_.points >= 10)
  val revealedDevCards: PlayedInventory = players.map(_.playedDevCards).fold( DevCardInventory.empty) (_.add(_))

  /**
    * State Transition Functions
    */

  def initialPlacement(playerId: Int, first: Boolean, vertex: Vertex, edge: Edge): GameState = {
    val newState = buildSettlement(playerId, vertex, false).buildRoad(playerId, edge, false)
    val resourcesFromSettlement = if(!first) {
      newState.board.adjacentHexes(vertex).flatMap { node =>
        node.hex.getResourceAndNumber.map {
          case (resource, _) => resource
        }
      }.foldLeft(CatanResourceSet.empty) { case (set, res) => set.add(1, res) }
    } else CatanResourceSet.empty
    val newTransactions = if (!resourcesFromSettlement.isEmpty) List(Gain(playerId, resourcesFromSettlement))
                          else Nil
    //val newPossHands = possibleHands.calculateHands(newTransactions)

//    log.print(s"player $playerId built ${if (first) "first" else "second"} placement on vertex: $vertex and edge: $edge " +
//      s"${if (!first) s"and gained ${CatanResourceSet.describe(resourcesFromSettlement)}"}")
    newState.copy (
      players = newState.players.map(_.updateResources(newTransactions)),
      bank = bank.subtract(resourcesFromSettlement),
     // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions
    )
  }


  /**
    * roll the dice and the players collect the resources
    * @param diceRoll the roll of the dice. integer from 2 to 12
    * @see roll7 if dice roll is a 7
    */
  def rollDice(playerId: Int, diceRoll: Roll): GameState = {

    //log.print(s"player $playerId rolled a ${diceRoll.number}")

    if (diceRoll.number == 7) return copy(diceRolls = diceRolls + 1, turnState = turnState.copy(canRollDice = false))

    val resForPlayers: Map[Int, Resources] = board.getResourcesGainedOnRoll(diceRoll.number)
    val totalResourcesCollected: Resources = resForPlayers.values.foldLeft(CatanResourceSet.empty)(_.add(_))
    val actualResForPlayers = if (!bank.contains(totalResourcesCollected)) {
      val overflowTypes = {
        val total = bank.subtract(totalResourcesCollected)
        Resource.list.filter(res => total.contains(res))
      }
      resForPlayers.map { case (player, resourceSet) =>
        player -> overflowTypes.foldLeft(resourceSet) { case (set, res) =>
          set.subtract(set.getAmount(res), res)
        }
      }
    } else resForPlayers
    val trueTotalCollected = actualResForPlayers.values.foldLeft(CatanResourceSet.empty)(_.add(_))

    val newTransactions: List[Gain] = players.map { player =>
      Gain(player.position, actualResForPlayers.getOrElse(player.position, CatanResourceSet()))
    }.filterNot(_.resourceSet.isEmpty)

    //val newPossHands = possibleHands.calculateHands(newTransactions)
    //log.print(newTransactions.map(g => s" player ${g.playerId} gained ${CatanResourceSet.describe(g.resourceSet)}").mkString("\n"))

    copy(
      players = players.map (_.updateResources(newTransactions)),
      bank = bank.subtract(trueTotalCollected),
     // possibleHands = newPossHands,
      turnState = turnState.copy(canRollDice = false),
      transactions = transactions ::: newTransactions,
      diceRolls = diceRolls + 1
    )
  }

  def playersDiscardFromSeven(cardsLostMap: Map[Int, Resources]): GameState = {
    val newTransactions = cardsLostMap.map { case (player, discard) => Lose(player, discard)}.toList
    //val newPossHands = possibleHands.calculateHands(newTransactions)
    val totalLost = cardsLostMap.values.foldLeft(CatanResourceSet.empty)(_.add(_))

    copy (
      bank = bank.add(totalLost),
     // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions,
      players = players.map(_.updateResources(newTransactions))
    )

  }

  /**
    * when a 7 is rolled move the robber to a new location.
    * Steal a card from a player on the hex of the new robber location.
    * If players have more than 7 cards indicate which cards they discarding
    * @param robberLocation the new robber location
    * @param playerStole the player being stolen from
    * @param cardsLost which players lost which cards
    */
  def moveRobberAndSteal(playerId: Int, robberLocation: Int, steal: Option[Steal]): GameState = {
    //log.print(s"player $playerId moved robber to $robberLocation ${steal.fold("")(v => s"and stole from player $v")}")
    val newTransactions = steal.fold(List.empty[SOCTransactions])(s => List(s))
   // val newPossHands = possibleHands.calculateHands(newTransactions)

    copy(
      board = board.copy(robberHex = robberLocation),
     // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions,
      players = players.map(_.updateResources(newTransactions))
    )
  }

  def buildSettlement(playerId: Int, vertex: Vertex, buy: Boolean = true): GameState = {
    //log.print(s"player $playerId built a settlement on vertex $vertex")

    val newBoard = board.buildSettlement(vertex, playerId)
    val newTransactions = if (buy) List(Lose(playerId, Settlement.cost)) else Nil
    //val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.map {
        case player if player.position == playerId => player.buildSettlement(newBoard, vertex)
        case player => player
      }.map(_.updateResources(newTransactions)),
      board = newBoard,
     // possibleHands = newPossHands,
      bank = if (buy) bank.add(Settlement.cost) else bank,
      transactions = transactions ::: newTransactions
    )
  }

  def buildCity(playerId: Int, vertex: Vertex): GameState  = {
    val newBoard = board.buildCity(vertex, playerId)
    val newTransactions = List(Lose(playerId, City.cost))
    //val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.map {
        case player if player.position == playerId => player.buildCity(newBoard, vertex)
        case player => player
      }.map(_.updateResources(newTransactions)),
      board = newBoard,
     // possibleHands = newPossHands,
      bank = bank.add(City.cost),
      transactions = transactions ::: newTransactions
    )
  }

  def buildRoad(playerId: Int, edge: Edge, buy: Boolean = true): GameState = {
    val newBoard = board.buildRoad(edge, playerId)
    val currPlayerRoadLength = newBoard.longestRoadLength(playerId)
    val newTransactions = if (buy) List(Lose(playerId, Road.cost)) else Nil

    val longestRoad = players.find(_.roadPoints >= 2)
    val playerLostRoad: Option[PlayerState] = longestRoad match {
      case Some(p) if p.position == playerId => None
      case Some(player) if currPlayerRoadLength > player.roadLength => Some(player)
      case _ => None
    }
    val currPlayerGainRoad: Boolean = (longestRoad, playerLostRoad) match {
      case (None, _) if currPlayerRoadLength >= 5 => true
      case (Some(p), _) if p.position == playerId => false
      case (Some(_), None) => false
      case (Some(playerHad), Some(playerLost)) if playerHad == playerLost => true
      case (None, None) => false
    }
    val newCurrPlayer = {
      val currPlayer = getPlayer(playerId)
      if (currPlayerGainRoad) currPlayer.buildRoad(newBoard, edge).gainLongestRoad
      else currPlayer.buildRoad(newBoard, edge)
    }
    val otherPlayers = players.filterNot(_.position == playerId).map { player =>
      playerLostRoad match {
        case Some(`player`) => player.loseLongestRoad
        case _ => player
      }
    }
    copy(
      players = (newCurrPlayer :: otherPlayers).map(_.updateResources(newTransactions)),
      board = newBoard,
     // possibleHands = newPossHands,
      bank = if (buy) bank.add(Road.cost) else bank,
      transactions = transactions ::: newTransactions
    )
  }

  def buyDevelopmentCard(playerId: Int, card: Option[DevelopmentCard]): GameState = {
    val newTransactions = List(Lose(playerId, DevelopmentCard.cost))
   // val newPossHands = possibleHands.calculateHands(newTransactions)

    val newPlayers = players.map {
      case player if player.position == playerId => player.buyDevelopmentCard(card)
      case player => player
    }.map(_.updateResources(newTransactions))

    copy(
      players = newPlayers,
     // possibleHands = newPossHands,
      bank = bank.add(DevelopmentCard.cost),
      transactions = transactions ::: newTransactions,
      devCardsDeck = devCardsDeck - 1
    )
  }

  def playKnight(playerId: Int, robberLocation: Int, steal: Option[Steal]): GameState = {
    val currPlayerKnightCount = getPlayer(playerId).playedDevCards(Knight) + 1
    val largestArmy = players.find(_.armyPoints >= 2)
    val playerLostArmy = largestArmy match {
      case Some(p) if p.position == playerId => None
      case Some(p) if currPlayerKnightCount > p.playedDevCards(Knight) => Some(p)
      case _ => None
    }
    val currPlayerGainedArmy = (largestArmy, playerLostArmy) match {
      case(None, _) if currPlayerKnightCount >= 3 => true
      case (Some(p), _) if p.position == playerId => false
      case (Some(_), None) => false
      case (Some(playerHad), Some(playerLost)) if playerHad == playerLost => true
      case (None, None) => false
    }
    val newCurrPlayer = {
      val currPlayer = players.find(_.position == playerId).get
      if (currPlayerGainedArmy) currPlayer.playDevelopmentCard(Knight).gainLargestArmy
      else currPlayer.playDevelopmentCard(Knight)
    }
    val otherPlayers = players.filterNot(_.position == playerId).map { player =>
      playerLostArmy match {
        case Some(`player`) => player.loseLargestArmy
        case _ => player
      }
    }
    copy(
      players = (newCurrPlayer :: otherPlayers),
      turnState = turnState.copy(canPlayDevCard = false)).moveRobberAndSteal(playerId, robberLocation, steal)
  }

  def playMonopoly(playerId: Int, cardsLost: Map[Int, Resources]) = {
    val newTransactions = Gain(playerId, cardsLost.values.fold(CatanResourceSet.empty)(_.add(_))) ::
      cardsLost.map { case (player, cards) => Lose(player, cards)}.toList
   // val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.map {
        case player if player.position == playerId => player.playDevelopmentCard(Monopoly)
        case player => player
      }.map(_.updateResources(newTransactions)),
      turnState = turnState.copy(canPlayDevCard = false),
     // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions
    )
  }

  def playYearOfPlenty(playerId: Int, card1: Resource, card2: Resource) = {
    val set = CatanResourceSet.empty.add(1, card1).add(1, card2)
    val newTransactions = List(Gain(playerId, set))
   // val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.map {
        case player if player.position == playerId => player.playDevelopmentCard(YearOfPlenty)
        case player => player
      }.map(_.updateResources(newTransactions)),
      bank = bank.subtract(set),
   //   possibleHands = newPossHands,
      transactions = transactions ::: newTransactions,
      turnState = turnState.copy(canPlayDevCard = false)
    )
  }

  def playRoadBuilder(playerId: Int, road1: Edge, road2: Option[Edge]) = {
    val firstRoadBoard = buildRoad(playerId, road1, false)
    val newBoard = road2.fold(firstRoadBoard)(r => firstRoadBoard.buildRoad(playerId, r, false))
    newBoard.copy (
      players = players.map {
        case player if player.position == playerId => player.playDevelopmentCard(RoadBuilder)
        case player => player
      },
      turnState = turnState.copy(canPlayDevCard = false)
    )
  }

//  def trade(state: GameState, trade: Trade): GameState = {
//    val currentPlayer = state.players.head
//
//    val transactions = List(
//      Lose(currentPlayer.position, trade.socTrade.getGiveSet),
//      Lose(trade.socTrade.getFrom, trade.socTrade.getGetSet),
//      Gain(currentPlayer.position, trade.socTrade.getGetSet),
//      Gain(trade.socTrade.getFrom, trade.socTrade.getGiveSet)
//    )
//
//    val possibleHands = SOCPossibleHands.calculateHands(state.possibleHands, transactions)
//
//    state.copy(
//      possibleHands = possibleHands,
//      transactions = state.transactions ::: transactions
//    )
//
//  }

  def portTrade(playerId: Int, give: Resources, get: Resources): GameState = {

    val newTransactions = List(
      Lose(playerId, give),
      Gain(playerId, get)
    )

   // val newPossibleHands = possibleHands.calculateHands(newTransactions)
    copy (
      bank = bank.subtract(get).add(give),
    //  possibleHands = newPossibleHands,
      transactions = transactions ::: newTransactions,
      players = players.map(_.updateResources(newTransactions))
    )
  }

  def endTurn(playerId: Int) = copy(
    players = players.map {
      case player if player.position == playerId => player.endTurn
      case player => player
    },
    turnState = TurnState()
  )


  case class StateWithProbability(state: GameState, prob: Double = 1.0)
  def getPossibleStatesForMove(playerId: Int, move: CatanMove): Iterator[StateWithProbability] = move match {

    case InitialPlacementMove(first, settlement, road) => Iterator(StateWithProbability(initialPlacement(playerId, first, settlement, road)))
    case BuildRoad(edge) => Iterator(StateWithProbability(buildRoad(playerId, edge)))
    case BuildSettlement(vertex) => Iterator(StateWithProbability(buildSettlement(playerId, vertex)))
    case BuildCity(vertex) => Iterator(StateWithProbability(buildCity(playerId, vertex)))
    case PortTrade(give, get) => Iterator(StateWithProbability(portTrade(playerId, give, get)))

    case YearOfPlentyMove(res1, res2) => Iterator(StateWithProbability(playYearOfPlenty(playerId, res1, res2)))
    case RoadBuilderMove(road1, road2) => Iterator(StateWithProbability(playRoadBuilder(playerId, road1, road2)))
    case EndTurnMove => Iterator(StateWithProbability(endTurn(playerId)))

    case RollDiceMove => (2 to 12).toIterator.flatMap {
      case r if r != 7 =>
        val roll = Roll(r)
        Iterator(StateWithProbability(rollDice(playerId, roll), roll.prob))
      case 7 =>
        CatanPossibleMoves(this, playerId).getPossibleDiscards()

    }

  }

}

