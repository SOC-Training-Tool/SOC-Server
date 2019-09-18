package soc.game

import soc.game.board.{CatanBoard, Edge, Vertex}
import soc.game.inventory.Inventory
import soc.game.inventory._
import soc.game.player.PlayerStateHelper
import soc.game.inventory.resources.CatanResourceSet._
import soc.game.inventory.resources.{CatanResourceSet, Gain, Lose, SOCTransactions, Steal}

case class GameState[T <: Inventory[T]](
  board: CatanBoard,
  players: PlayerStateHelper[T],
  currTurn: Int,
  turnState: TurnState = TurnState(),
  bank: Resources,
  devCardsDeck: Int,
  transactions: List[SOCTransactions] = Nil,
  diceRolls: Int = 0
) {

  val turn = diceRolls / 4

  val firstPlayerId = players.firstPlayerId
  val lastPlayerId = players.lastPlayerId

  //val gameOver = players.getPlayers.exists(_.points >= 10)

  //def apply(playerId: Int, moveResult: MoveResult[_]): GameState[M, P] = moveResult.applyMove(playerId, this)

  /**
    * State Transition Functions
    */

  def initialPlacement(result: InitialPlacementMove): GameState[T] = initialPlacement(result.first, result.settlement, result.road)
  def initialPlacement(first: Boolean, vertex: Vertex, edge: Edge): GameState[T] = {

    val newState = buildSettlement(vertex, false).buildRoad(edge, false)
    val resourcesFromSettlement = if (!first) {
      val resList = newState.board.adjacentHexes(vertex).flatMap { node =>
        node.hex.getResourceAndNumber.map {
          case (resource, _) => resource
        }
      }
      CatanResourceSet(resList:_*)
    } else CatanResourceSet.empty[Int]
    val newTransactions = if (!resourcesFromSettlement.isEmpty) List(Gain(currTurn, resourcesFromSettlement))
    else Nil
    //val newPossHands = possibleHands.calculateHands(newTransactions)

    //    log.print(s"player $playerId built ${if (first) "first" else "second"} placement on vertex: $vertex and edge: $edge " +
    //      s"${if (!first) s"and gained ${CatanResourceSet.describe(resourcesFromSettlement)}"}")

    val nextTurn = (first, currTurn) match {
      case (true, `lastPlayerId`) => currTurn
      case (true, _) => players.nextPlayer(currTurn)
      case (false, `firstPlayerId`) => currTurn
      case (false, _) => players.previousPlayer(currTurn)
    }

    newState.copy(
      players = newState.players.updateResources(transactions),
      bank = bank.subtract(resourcesFromSettlement),
      // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions,
      currTurn = nextTurn
    )
  }


  /**
    * roll the dice and the players collect the resources
    *
    * @param diceRoll the roll of the dice. integer from 2 to 12
    * @see roll7 if dice roll is a 7
    */
  def rollDice( rollResult: RollResult): GameState[T] = rollDice(rollResult.roll)
  def rollDice(diceRoll: Roll): GameState[T] = {

    //log.print(s"player $playerId rolled a ${diceRoll.number}")

    if (diceRoll.number == 7) return copy(diceRolls = diceRolls + 1, turnState = turnState.copy(canRollDice = false))

    val resForPlayers: Map[Int, Resources] = board.getResourcesGainedOnRoll(diceRoll.number)
    val totalResourcesCollected: Resources = resForPlayers.values.foldLeft(CatanResourceSet.empty[Int])(_.add(_))
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
    val trueTotalCollected = actualResForPlayers.values.foldLeft(CatanResourceSet.empty[Int])(_.add(_))

    val newTransactions: List[Gain] = players.getPlayers.map { player =>
      Gain(player.position, actualResForPlayers.getOrElse(player.position, CatanResourceSet()))
    }.filterNot(_.resourceSet.isEmpty).toList

    //val newPossHands = possibleHands.calculateHands(newTransactions)
    //log.print(newTransactions.map(g => s" player ${g.playerId} gained ${CatanResourceSet.describe(g.resourceSet)}").mkString("\n"))

    copy(
      players = players.updateResources(newTransactions),
      bank = bank.subtract(trueTotalCollected),
      // possibleHands = newPossHands,
      turnState = turnState.copy(canRollDice = false),
      transactions = transactions ::: newTransactions,
      diceRolls = diceRolls + 1
    )
  }

  def playersDiscardFromSeven(discard: DiscardResourcesMove): GameState[T] = playersDiscardFromSeven(discard.resourceSet)
  def playersDiscardFromSeven(cardsLostMap: Map[Int, Resources]): GameState[T] = {
    val newTransactions = cardsLostMap.map { case (player, discard) => Lose(player, discard) }.toList
    //val newPossHands = possibleHands.calculateHands(newTransactions)
    val totalLost = cardsLostMap.values.foldLeft(CatanResourceSet.empty[Int])(_.add(_))

    copy(
      bank = bank.add(totalLost),
      // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions,
      players = players.updateResources(newTransactions)
    )

  }

  /**
    * when a 7 is rolled move the robber to a new location.
    * Steal a card from a player on the hex of the new robber location.
    * If players have more than 7 cards indicate which cards they discarding
    *
    * @param robberLocation the new robber location
    */
  def moveRobberAndSteal(result: MoveRobberAndStealResult): GameState[T] = moveRobberAndSteal(result.robberLocation, result.steal)
  def moveRobberAndSteal(robberLocation: Int, steal: Option[Steal]): GameState[T] = {
    //log.print(s"player $playerId moved robber to $robberLocation ${steal.fold("")(v => s"and stole from player $v")}")
    val newTransactions = steal.fold(List.empty[SOCTransactions])(s => List(s))
    // val newPossHands = possibleHands.calculateHands(newTransactions)

    copy(
      board = board.copy(robberHex = robberLocation),
      // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions,
      players = players.updateResources(newTransactions)
    )
  }

  def buildSettlement(result: BuildSettlementMove): GameState[T] = buildSettlement(result.vertex)
  def buildSettlement(vertex: Vertex, buy: Boolean = true): GameState[T] = {
    //log.print(s"player $playerId built a settlement on vertex $vertex")

    val newBoard = board.buildSettlement(vertex, currTurn)
    val newTransactions = if (buy) List(Lose(currTurn, Settlement.cost)) else Nil
    //val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.buildSettlement(currTurn, vertex, newBoard).updateResources(newTransactions),
      board = newBoard,
      // possibleHands = newPossHands,
      bank = if (buy) bank.add(Settlement.cost) else bank,
      transactions = transactions ::: newTransactions
    )
  }

  def buildCity(result: BuildCityMove): GameState[T] = buildCity(result.vertex)
  def buildCity(vertex: Vertex): GameState[T]  = {
    val newBoard = board.buildCity(vertex, currTurn)
    val newTransactions = List(Lose(currTurn, City.cost))
    //val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.buildCity(currTurn, vertex, newBoard).updateResources(newTransactions),
      board = newBoard,
      // possibleHands = newPossHands,
      bank = bank.add(City.cost),
      transactions = transactions ::: newTransactions
    )
  }

  def buildRoad(result: BuildRoadMove): GameState[T] = buildRoad(result.edge)
  def buildRoad(edge: Edge, buy: Boolean = true): GameState[T] = {
    val newBoard = board.buildRoad(edge, currTurn)
    val newTransactions = if (buy) List(Lose(currTurn, Road.cost)) else Nil

    copy(
      players = players.buildRoad(currTurn, edge, newBoard).updateResources(newTransactions),
      board = newBoard,
      // possibleHands = newPossHands,
      bank = if (buy) bank.add(Road.cost) else bank,
      transactions = transactions ::: newTransactions
    )
  }

  def buyDevelopmentCard(result: BuyDevelopmentCardResult): GameState[T] = buyDevelopmentCard(result.card)
  def buyDevelopmentCard(card: Option[DevelopmentCard]): GameState[T] = {
    val newTransactions = List(Lose(currTurn, DevelopmentCard.cost))
    // val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.buyDevelopmentCard(currTurn, card).updateResources(newTransactions),
      // possibleHands = newPossHands,
      bank = bank.add(DevelopmentCard.cost),
      transactions = transactions ::: newTransactions,
      devCardsDeck = devCardsDeck - 1
    )
  }

  def playKnight(result: KnightResult): GameState[T] = playKnight(result.robber.robberLocation, result.robber.steal)
  def playKnight(robberLocation: Int, steal: Option[Steal]): GameState[T]  = {
    copy(
      players = players.playKnight(currTurn),
      turnState = turnState.copy(canPlayDevCard = false)
    ).moveRobberAndSteal(robberLocation, steal)
  }

  def playMonopoly(result: MonopolyResult): GameState[T] = playMonopoly(result.cardsLost)
  def playMonopoly(cardsLost: Map[Int, Resources]): GameState[T] = {
    val newTransactions = Gain(currTurn, cardsLost.values.fold(CatanResourceSet.empty[Int])(_.add(_))) ::
      cardsLost.map { case (player, cards) => Lose(player, cards) }.toList
    // val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.playMonopoly(currTurn).updateResources(newTransactions),
      turnState = turnState.copy(canPlayDevCard = false),
      // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions
    )
  }

  def playYearOfPlenty(result: YearOfPlentyMove): GameState[T] = playYearOfPlenty(result.res1, result.res2)
  def playYearOfPlenty(card1: Resource, card2: Resource): GameState[T] = {
    val set = CatanResourceSet(card1, card2)
    val newTransactions = List(Gain(currTurn, set))
    // val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.playYearOfPlenty(currTurn).updateResources(newTransactions),
      bank = bank.subtract(set),
      //   possibleHands = newPossHands,
      transactions = transactions ::: newTransactions,
      turnState = turnState.copy(canPlayDevCard = false)
    )
  }

  def playRoadBuilder(result: RoadBuilderMove): GameState[T] = playRoadBuilder(result.road1, result.road2)
  def playRoadBuilder(road1: Edge, road2: Option[Edge]): GameState[T] = {
    val firstRoadBoard = buildRoad(road1, false)
    val newBoard = road2.fold(firstRoadBoard)(r => firstRoadBoard.buildRoad(r, false))
    newBoard.copy(
      players = players.playRoadBuilder(currTurn),
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

  def portTrade(result: PortTradeMove): GameState[T] = portTrade(result.give, result.get)
  def portTrade(give: Resources, get: Resources): GameState[T] = {

    val newTransactions = List(
      Lose(currTurn, give),
      Gain(currTurn, get)
    )

    // val newPossibleHands = possibleHands.calculateHands(newTransactions)
    copy(
      bank = bank.subtract(get).add(give),
      //  possibleHands = newPossibleHands,
      transactions = transactions ::: newTransactions,
      players = players.updateResources(newTransactions)
    )
  }

  def endTurn: GameState[T] = copy(
    players = players.endTurn(currTurn),
    turnState = TurnState(),
    currTurn = players.nextPlayer(currTurn)
  )

  def apply(moveResult: MoveResult): GameState[T] = moveResult match {
    case r: RollResult => rollDice(r)
    case EndTurnMove => endTurn
    case r: InitialPlacementMove => initialPlacement(r)
    case r: MoveRobberAndStealResult => moveRobberAndSteal(r)
    case r: BuyDevelopmentCardResult => buyDevelopmentCard(r)
    case r: BuildRoadMove => buildRoad(r)
    case r: BuildSettlementMove => buildSettlement(r)
    case r: BuildCityMove => buildCity(r)
    case r: PortTradeMove => portTrade(r)
    case r: KnightResult => playKnight(r)
    case r: YearOfPlentyMove => playYearOfPlenty(r)
    case r: MonopolyResult => playMonopoly(r)
    case r: RoadBuilderMove => playRoadBuilder(r)
    case _ => this
  }
}

object GameState {

  def apply[T <: Inventory[T]](
    board: CatanBoard,
    playerNameIds: Seq[(String, Int)],
    rules: GameRules
  )(implicit gameInventoryHelperFactory: InventoryHelperFactory[T]): GameState[T] = {
    val players = PlayerStateHelper[T](playerNameIds)(gameInventoryHelperFactory, rules)
    GameState[T](
      board,
      players,
      players.firstPlayerId,
      TurnState(),
      rules.initBank,
      rules.initDevCardAmounts.getTotal
    )
  }
}