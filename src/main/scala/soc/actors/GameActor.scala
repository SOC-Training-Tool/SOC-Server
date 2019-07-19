//package soc.actors
//
//import akka.actor._
//import log.Log
//import soc.actors.GameActor._
//import soc.game.board.{CatanBoard, Edge, Vertex}
//import soc.game._
//import soc.game.dice.{Dice, NormalDice}
//import soc.game.messages.InitialPlacementRequest
//import soc.game.messages.UpdateMessage._
//import soc.game.player.{PerfectInfoPlayerState, PlayerState}
//import soc.game.resources.CatanResourceSet.Resources
//import soc.game.resources.{CatanResourceSet, Steal}
//
//import scala.util.Random
//
//class GameActor(board: CatanBoard, dice: Dice, log: Log)(implicit random: Random) extends Actor {
//
//  var gameState: GameState = _
//  var dCardDeck: List[DevelopmentCard] = DevelopmentCardDeckBuilder.buildDeckByCardTypeAndAmount(
//    Map(Knight -> Knight.initAmount,
//        Point -> Point.initAmount,
//        RoadBuilder -> RoadBuilder.initAmount,
//        Monopoly -> Monopoly.initAmount,
//        YearOfPlenty -> YearOfPlenty.initAmount)
//  )
//
//  lazy val firstPosition: Int = playerActors.keys.min
//  lazy val lastPosition: Int = playerActors.keys.max
//  lazy val playersSorted = playerActors.keys.toSeq.sorted
//
//  var playerNames: Map[Int, String] = Map.empty
//  var playerActors: Map[Int, ActorRef] = Map.empty
//
//  var discarding: Option[DiscardedCardsMapBuilder] = None
//
//  var numTurns = 0
//
//
//  def nextPlayer(playerId: Int): Int = {
//    val indexOf = playersSorted.indexOf(playerId)
//    playersSorted.drop(indexOf + 1).headOption.getOrElse(firstPosition)
//  }
//  def previousPlayer(playerId: Int): Int = {
//    val indexOf = playersSorted.indexOf(playerId)
//    playersSorted.dropRight(playersSorted.length - indexOf).lastOption.getOrElse(lastPosition)
//  }
//
//  //TODO update state
//  override def receive: Receive = {
//    case AddPlayer(playerId, name, ref) =>
//      playerNames += (playerId -> name)
//      playerActors += (playerId -> ref)
//      //log.print(s"player: $name has been added in position $playerId")
//      sendToAllPlayers(PlayerAdded(playerId))
//
//    case StartGame =>
//
//      val players: List[PlayerState] = playerNames.map {
//        case (playerId, name) => PerfectInfoPlayerState(name, playerId)
//      }.toList.sortBy(_.position)
//      gameState = GameState(board, players)
//      //log.print("Starting Game")
//      sendToAllPlayers(StartingGame(board, playerNames))
//      sendToAllPlayers(InitialPlacementRequest(0, true))
//
//    case Response(`lastPosition`, InitialPlacementResponse(true, settlement, road)) =>
//      gameState = gameState.initialPlacement(lastPosition, true, settlement, road)
//      sendToAllPlayers(InitialPlacementUpdate(lastPosition, true, settlement, road))
//      sendToAllPlayers(InitialPlacementRequest(lastPosition, false))
//
//    case Response(position, InitialPlacementResponse(true, settlement, road)) =>
//      gameState = gameState.initialPlacement(position, true, settlement, road)
//      sendToAllPlayers(InitialPlacementUpdate(position, true, settlement, road))
//      sendToAllPlayers(InitialPlacementRequest(nextPlayer(position), true))
//
//    case Response(`firstPosition`, InitialPlacementResponse(false, settlement, road)) =>
//      gameState = gameState.initialPlacement(firstPosition, false, settlement, road)
//      //log.print(s"its player $firstPosition's turn")
//      sendToAllPlayers(InitialPlacementUpdate(0, false, settlement, road))
//      sendToAllPlayers(TurnUpdate(firstPosition, numTurns))
//      numTurns += 1
//
//    case Response(position, InitialPlacementResponse(false, settlement, road)) =>
//      gameState = gameState.initialPlacement(position, false, settlement, road)
//      sendToAllPlayers(InitialPlacementUpdate(position, false, settlement, road))
//      sendToAllPlayers(InitialPlacementRequest(previousPlayer(position), false))
//
//    case Response(playerId, RollDiceResponse) =>
//      val (roll1, roll2) = dice.getRoll
//      val roll = Roll(roll1 + roll2)
//      val resourceGained = gameState.board.getResourcesGainedOnRoll(roll.number)
//      gameState = gameState.rollDice(playerId, roll)
//      sendToAllPlayers(RollDiceUpdate(playerId, roll, resourceGained))
//      if (roll.number == 7) {
//          val toDiscard = gameState.players.filter(_.numCards > 7).map(_.position)
//          if (!toDiscard.isEmpty) {
//            discarding = Some(DiscardedCardsMapBuilder(toDiscard))
//            //log.print(s"players ${toDiscard.mkString(",")} need to discard cards")
//            sendToAllPlayers(DiscardCardRequest(toDiscard))
//          }
//          else
//            sendToAllPlayers(MoveRobberRequest(playerId))
//      }
//
//    case Response(playerId, DiscardCardsResponse(cards)) =>
//      discarding = discarding.map(_.addDiscard(playerId, cards))
//      if (!discarding.get.expectingDiscard) {
//        gameState = gameState.playersDiscardFromSeven(discarding.get.cardsToDiscard)
//        sendToAllPlayers(DiscardCardsUpdate(discarding.get.cardsToDiscard))
//        //log.print(s"players are discarding: ${discarding.get.cardsToDiscard}")
//        discarding = None
//        sendToAllPlayers(MoveRobberRequest(gameState.currPlayer.position))
//      }
//
//    case Response(playerId, MoveRobberResponse(robberLocation, victim)) =>
//      //log.print(s"player $playerId moved robber to $robberLocation ${victim.fold("")(v => s"and stole from player $v")}")
//      stealCard(playerId, robberLocation, victim)(gameState.moveRobberAndSteal)(MoveRobberUpdate)
//
//    case Response(playerId, BuildSettlementResponse(settlement)) =>
//      //log.print(s"player $playerId built a settlement on vertex $settlement")
//      gameState = gameState.buildSettlement(playerId, settlement)
//      sendToAllPlayers(BuildSettlementUpdate(playerId, settlement))
//
//    case Response(playerId, BuildCityResponse(city)) =>
//      //log.print(s"player $playerId built a city on vertex $city")
//      gameState = gameState.buildCity(playerId, city)
//      sendToAllPlayers(BuildCityUpdate(playerId, city))
//
//    case Response(playerId, BuildRoadResponse(road)) =>
//      //log.print(s"player $playerId built a road on edge $road")
//      def longest =
//      val hadLongest = longest
//      gameState = gameState.buildRoad(playerId, road)
//      if (!hadLongest && longest) {
//        //log.print(s"player $playerId gained longest road")
//        sendToAllPlayers(LongestRoadUpdate(playerId))
//      }
//      sendToAllPlayers(BuildRoadUpdate(playerId, road))
//
//    case Response(playerId, BuyDevelopmentCardResponse) =>
//      //TODO check if deck is empty
//      //log.print(s"player $playerId bought a development card")
//      sendToSomePlayers(gameState.players.map(_.position).filterNot(_ == playerId), BuyDevelopmentCardUpdate(playerId, None))
//      sendToSomePlayers(List(playerId), BuyDevelopmentCardUpdate(playerId, Some(dCardDeck.head)))
//      dCardDeck = dCardDeck.tail
//
//    case Response(playerId, PlayDevelopmentCardResponse(KnightMove(robber))) =>
//      stealCard(playerId, robber.node, robber.playerStole)(gameState.playKnight)(KnightUpdate)
//
//    case Response(playerId, PlayDevelopmentCardResponse(YearOfPlentyMove(res1, res2))) =>
//      gameState = gameState.playYearOfPlenty(playerId, res1, res2)
//      sendToAllPlayers(YearOfPlentyUpdate(playerId, res1, res2))
//
//    case Response(playerId, PlayDevelopmentCardResponse(MonopolyMove(res))) =>
//      val cardsLost = gameState.players.map { p =>
//        p.position -> CatanResourceSet.empty.add(p.getAmount(res).toInt, res)
//      }.toMap
//      gameState = gameState.playMonopoly(playerId, cardsLost)
//      sendToAllPlayers(MonopolyUpdate(playerId, cardsLost))
//
//    case Response(playerId, PlayDevelopmentCardResponse(RoadBuilderMove(road1, road2))) =>
//      gameState = gameState.playRoadBuilder(playerId, road1, road2)
//      sendToAllPlayers(RoadBuilderUpdate(playerId, road1, road2))
//
//    case Response(playerId, PortTradeResponse(give, get)) =>
//      gameState = gameState.portTrade(playerId, PortTrade(give, get))
//      sendToAllPlayers(PortTradeUpdate(playerId, PortTrade(give, get)))
//
//    case Response(playerId, EndTurnResponse) =>
//      //log.print(s"player $playerId ended their turn\n")
//      //log.print(s"player ${nextPlayer(playerId)}'s turn")
//      gameState = gameState.endTurn(playerId)
//      sendToAllPlayers(EndTurnUpdate(playerId))
//      sendToAllPlayers(TurnUpdate(nextPlayer(playerId), numTurns))
//      numTurns += 1
//  }
//
//  var messageCount = 0
//
//  def sendToAllPlayers(message: Any) = {
//    log.print(s"$messageCount, ${playerActors.keys.mkString(",")}->$message")
//    playerActors.values.foreach (_ ! message)
//    messageCount += 1
//  }
//  def sendToSomePlayers(to: List[Int], message: Any) = {
//    log.print(s"$messageCount, ${to.mkString(",")}-> $message")
//    playerActors.values.filter(to.contains).foreach(_ ! message)
//    messageCount += 1
//  }
//
//  def stealCard(playerId: Int, robberLocation: Int, steal: Option[Int])
//    (gameStateUpdater: (Int, Int, Option[Steal]) => GameState)
//    (message: (Int, Int, Option[Steal]) => Any) = steal match {
//    case Some(victim) =>
//      val involvedPlayers = List(playerId, victim)
//      val stolenRes = gameState.getPlayer(victim).getRandomCard
//      val knownSteal = stolenRes.map(res => Steal(playerId, victim, Some(CatanResourceSet.empty.add(1, res))))
//      val unknownSteal = Some(Steal(playerId, victim, None))
//      gameState = gameStateUpdater(playerId, robberLocation, knownSteal)
//      sendToSomePlayers(involvedPlayers, message(playerId, robberLocation, knownSteal))
//      sendToSomePlayers(gameState.players.map(_.position).filterNot(involvedPlayers.contains), message(playerId, robberLocation, unknownSteal))
//    case None =>
//      gameState = gameStateUpdater(playerId, robberLocation, None)
//      sendToAllPlayers(message(playerId, robberLocation, None))
//  }
//}
//
//object GameActor {
//
//  def props(board: CatanBoard, dice: Dice, log: Log)(implicit random: Random) = Props(new GameActor(board, dice, log))
//
//}
//
//case class DiscardedCardsMapBuilder private(playersToDiscard: List[Int], cardsToDiscard: Map[Int, Resources] = Map.empty) {
//
//  val waitingFor = playersToDiscard.filterNot(cardsToDiscard.contains)
//  val expectingDiscard = !waitingFor.isEmpty
//
//  def addDiscard(playerId: Int, cards: Resources): DiscardedCardsMapBuilder = copy(cardsToDiscard = cardsToDiscard + (playerId -> cards))
//
//}
//
//object DevelopmentCardDeckBuilder {
//  def buildDeckByCardTypeAndAmount(cardAmountMap: Map[DevelopmentCard, Int])(implicit random: Random): List[DevelopmentCard] = {
//    random.shuffle(cardAmountMap.flatMap { case (card, amount) => (1 to amount).map( _ => card) }).toList
//  }
//}
//
//
