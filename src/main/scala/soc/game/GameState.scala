package soc.game

import soc.game._
import soc.game.board.{CatanBoard, Edge, Vertex}
import soc.game.inventory.Inventory
import soc.game.inventory._
import soc.game.player.{PlayerState, PlayerStateManager}
import soc.game.inventory.resources.CatanResourceSet._
import soc.game.inventory.resources.{CatanResourceSet, Gain, Lose, SOCTransactions, Steal}
import soc.game.moves.{BuildCityMove, BuildRoadMove, BuildSettlementMove, BuyDevelopmentCardResult, DiscardResourcesMove, EndTurnMove, InitialPlacementMove, KnightResult, MonopolyResult, MoveResult, MoveRobberAndStealResult, PortTradeMove, RoadBuilderMove, RollResult, YearOfPlentyMove}

case class GameState[T <: Inventory[T]](
  board: CatanBoard,
  players: PlayerStateManager[T],
  turnState: TurnState = TurnState(),
  bank: Resources = CatanResourceSet.fullBank,
  devCardsDeck: Int = 25,
  currPlayer: Int = 0,
  transactions: List[SOCTransactions] = Nil,
  diceRolls: Int = 0
) {

  //def getPlayer(playerId: Int): PlayerState[M] = players.getPlayer(playerId)

  val turn = diceRolls / 4
  //
  //  def getStateArray: List[Double] = {
  //    val probableResourceSets = possibleHands.toProbableResourceSets
  //
  //    List(board.getStateArray, bank.getStateArray, List(devCardsDeck.toDouble)).flatten :::
  //      players.flatMap(p => p.getStateArray(probableResourceSets(p.position)))
  //  }


  //val gameOver = players.getPlayers.exists(_.points >= 10)

  //def apply(playerId: Int, moveResult: MoveResult[_]): GameState[M, P] = moveResult.applyMove(playerId, this)

  /**
    * State Transition Functions
    */

  def initialPlacement(playerId: Int, result: InitialPlacementMove): GameState[T] = initialPlacement(playerId, result.first, result.settlement, result.road)
  def initialPlacement(playerId: Int, first: Boolean, vertex: Vertex, edge: Edge): GameState[T] = {

    val newState = buildSettlement(playerId, vertex, false).buildRoad(playerId, edge, false)
    val resourcesFromSettlement = if (!first) {
      val resList = newState.board.adjacentHexes(vertex).flatMap { node =>
        node.hex.getResourceAndNumber.map {
          case (resource, _) => resource
        }
      }
      CatanResourceSet(resList:_*)
    } else CatanResourceSet.empty[Int]
    val newTransactions = if (!resourcesFromSettlement.isEmpty) List(Gain(playerId, resourcesFromSettlement))
    else Nil
    //val newPossHands = possibleHands.calculateHands(newTransactions)

    //    log.print(s"player $playerId built ${if (first) "first" else "second"} placement on vertex: $vertex and edge: $edge " +
    //      s"${if (!first) s"and gained ${CatanResourceSet.describe(resourcesFromSettlement)}"}")

    val nextPlayer = if (first) {
      if (currPlayer != players.lastPlayerId) players.nextPlayer(currPlayer) else players.lastPlayerId
    } else {
      if (currPlayer != players.firstPlayerId) players.previousPlayer(currPlayer) else players.firstPlayerId

    }

    newState.copy(
      players = newState.players.updateResources(transactions),
      bank = bank.subtract(resourcesFromSettlement),
      // possibleHands = newPossHands,
      currPlayer = nextPlayer,
      transactions = transactions ::: newTransactions
    )


  }


  /**
    * roll the dice and the players collect the resources
    *
    * @param diceRoll the roll of the dice. integer from 2 to 12
    * @see roll7 if dice roll is a 7
    */
  def rollDice(playerId: Int, rollResult: RollResult): GameState[T] = rollDice(playerId, rollResult.roll)
  def rollDice(playerId: Int, diceRoll: Roll): GameState[T] = {

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

  def playersDiscardFromSeven(playerId: Int, discard: DiscardResourcesMove): GameState[T] = playersDiscardFromSeven(discard.resourceSet)
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
  def moveRobberAndSteal(playerId: Int, result: MoveRobberAndStealResult): GameState[T] = moveRobberAndSteal(playerId, result.robberLocation, result.steal)
  def moveRobberAndSteal(playerId: Int, robberLocation: Int, steal: Option[Steal]): GameState[T] = {
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

  def buildSettlement(playerId: Int, result: BuildSettlementMove): GameState[T] = buildSettlement(playerId, result.vertex)
  def buildSettlement(playerId: Int, vertex: Vertex, buy: Boolean = true): GameState[T] = {
    //log.print(s"player $playerId built a settlement on vertex $vertex")

    val newBoard = board.buildSettlement(vertex, playerId)
    val newTransactions = if (buy) List(Lose(playerId, Settlement.cost)) else Nil
    //val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.buildSettlement(playerId, vertex, newBoard).updateResources(newTransactions),
      board = newBoard,
      // possibleHands = newPossHands,
      bank = if (buy) bank.add(Settlement.cost) else bank,
      transactions = transactions ::: newTransactions
    )
  }

  def buildCity(playerId: Int, result: BuildCityMove): GameState[T] = buildCity(playerId, result.vertex)
  def buildCity(playerId: Int, vertex: Vertex): GameState[T]  = {
    val newBoard = board.buildCity(vertex, playerId)
    val newTransactions = List(Lose(playerId, City.cost))
    //val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.buildCity(playerId, vertex, newBoard).updateResources(newTransactions),
      board = newBoard,
      // possibleHands = newPossHands,
      bank = bank.add(City.cost),
      transactions = transactions ::: newTransactions
    )
  }

  def buildRoad(playerId: Int, result: BuildRoadMove): GameState[T] = buildRoad(playerId, result.edge)
  def buildRoad(playerId: Int, edge: Edge, buy: Boolean = true): GameState[T] = {
    val newBoard = board.buildRoad(edge, playerId)
    val newTransactions = if (buy) List(Lose(playerId, Road.cost)) else Nil

    copy(
      players = players.buildRoad(playerId, edge, newBoard).updateResources(newTransactions),
      board = newBoard,
      // possibleHands = newPossHands,
      bank = if (buy) bank.add(Road.cost) else bank,
      transactions = transactions ::: newTransactions
    )
  }

  def buyDevelopmentCard(playerId: Int, result: BuyDevelopmentCardResult): GameState[T] = buyDevelopmentCard(playerId, result.card)
  def buyDevelopmentCard(playerId: Int, card: Option[DevelopmentCard]): GameState[T] = {
    val newTransactions = List(Lose(playerId, DevelopmentCard.cost))
    // val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.buyDevelopmentCard(playerId, card).updateResources(newTransactions),
      // possibleHands = newPossHands,
      bank = bank.add(DevelopmentCard.cost),
      transactions = transactions ::: newTransactions,
      devCardsDeck = devCardsDeck - 1
    )
  }

  def playKnight(playerId: Int, result: KnightResult): GameState[T] = playKnight(playerId, result.robber.robberLocation, result.robber.steal)
  def playKnight(playerId: Int, robberLocation: Int, steal: Option[Steal]): GameState[T]  = {
    copy(
      players = players.playKnight(playerId),
      turnState = turnState.copy(canPlayDevCard = false)
    ).moveRobberAndSteal(playerId, robberLocation, steal)
  }

  def playMonopoly(playerId: Int, result: MonopolyResult): GameState[T] = playMonopoly(playerId, result.cardsLost)
  def playMonopoly(playerId: Int, cardsLost: Map[Int, Resources]): GameState[T] = {
    val newTransactions = Gain(playerId, cardsLost.values.fold(CatanResourceSet.empty[Int])(_.add(_))) ::
      cardsLost.map { case (player, cards) => Lose(player, cards) }.toList
    // val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.playMonopoly(playerId).updateResources(newTransactions),
      turnState = turnState.copy(canPlayDevCard = false),
      // possibleHands = newPossHands,
      transactions = transactions ::: newTransactions
    )
  }

  def playYearOfPlenty(playerId: Int, result: YearOfPlentyMove): GameState[T] = playYearOfPlenty(playerId, result.res1, result.res2)
  def playYearOfPlenty(playerId: Int, card1: Resource, card2: Resource): GameState[T] = {
    val set = CatanResourceSet(card1, card2)
    val newTransactions = List(Gain(playerId, set))
    // val newPossHands = possibleHands.calculateHands(newTransactions)
    copy(
      players = players.playYearOfPlenty(playerId).updateResources(newTransactions),
      bank = bank.subtract(set),
      //   possibleHands = newPossHands,
      transactions = transactions ::: newTransactions,
      turnState = turnState.copy(canPlayDevCard = false)
    )
  }

  def playRoadBuilder(playerId: Int, result: RoadBuilderMove): GameState[T] = playRoadBuilder(playerId, result.road1, result.road2)
  def playRoadBuilder(playerId: Int, road1: Edge, road2: Option[Edge]): GameState[T] = {
    val firstRoadBoard = buildRoad(playerId, road1, false)
    val newBoard = road2.fold(firstRoadBoard)(r => firstRoadBoard.buildRoad(playerId, r, false))
    newBoard.copy(
      players = players.playRoadBuilder(playerId),
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

  def portTrade(playerId: Int, result: PortTradeMove): GameState[T] = portTrade(playerId, result.give, result.get)
  def portTrade(playerId: Int, give: Resources, get: Resources): GameState[T] = {

    val newTransactions = List(
      Lose(playerId, give),
      Gain(playerId, get)
    )

    // val newPossibleHands = possibleHands.calculateHands(newTransactions)
    copy(
      bank = bank.subtract(get).add(give),
      //  possibleHands = newPossibleHands,
      transactions = transactions ::: newTransactions,
      players = players.updateResources(newTransactions)
    )
  }

  def endTurn(playerId: Int): GameState[T] = copy(
    players = players.endTurn(playerId),
    currPlayer = players.nextPlayer(currPlayer),
    turnState = TurnState()
  )

  def apply(playerId: Int, moveResult: MoveResult): GameState[T] = moveResult match {
    case r: RollResult => rollDice(playerId, r)
    case EndTurnMove => endTurn(playerId)
    case r: InitialPlacementMove => initialPlacement(playerId, r)
    case r: MoveRobberAndStealResult => moveRobberAndSteal(playerId, r)
    case r: BuyDevelopmentCardResult => buyDevelopmentCard(playerId, r)
    case r: BuildRoadMove => buildRoad(playerId, r)
    case r: BuildSettlementMove => buildSettlement(playerId, r)
    case r: BuildCityMove => buildCity(playerId, r)
    case r: PortTradeMove => portTrade(playerId, r)
    case r: KnightResult => playKnight(playerId, r)
    case r: YearOfPlentyMove => playYearOfPlenty(playerId, r)
    case r: MonopolyResult => playMonopoly(playerId, r)
    case r: RoadBuilderMove => playRoadBuilder(playerId, r)
    case _ => this
  }
}