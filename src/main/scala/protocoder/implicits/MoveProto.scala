package protocoder.implicits

import protos.soc.moves.GameAction._
import protos.soc.moves.{BuyDevelopmentCardResult => PBuyDevResult, _}
import soc.board.Vertex
import soc.moves._
import protocoder.ProtoCoder
import ProtoCoder.ops._
import protocoder.implicits.ResourceProto._
import protocoder.implicits.BoardProto._
import protos.soc.inventory.Resource.UNKNOWN_RESOURCE
import protos.soc.inventory.ResourceCount
import protos.soc.moves.ActionResult.Result
import protos.soc.moves.ActionSpecification.Specification
import soc.board.BaseCatanBoard.baseBoardMapping
import soc.inventory.resources.SOCTransactions

object MoveProto {

  implicit def protoAction[U <: CatanMove]: ProtoCoder[U, ActionSpecification] = move => {
    import Specification._

      move match {
        case RollDiceMove => ActionSpecification(ROLL_DICE)
        case EndTurnMove => ActionSpecification(END_TURN)
        case InitialPlacementMove(first, settlement, road) =>
          ActionSpecification(INITIAL_PLACEMENT, InitialPlacementPayload(InitialPlacement(settlement.proto, road.proto, first)))
        case DiscardResourcesMove(res) =>
          ActionSpecification(DISCARD_RESOURCES, DiscardResourcesPayload(DiscardResources(res.proto)))
        case MoveRobberAndStealMove(node, playerStole) =>
          ActionSpecification(MOVE_ROBBER_AND_STEAL, MoveRobberAndStealPayload(MoveRobberAndSteal(Vertex(node).proto, playerStole)))
        case BuyDevelopmentCardMove => ActionSpecification(BUY_DEVELOPMENT_CARD)
        case BuildSettlementMove(vertex) =>
          ActionSpecification(BUILD_SETTLEMENT, BuildSettlementPayload(BuildSettlement(vertex.proto)))
        case BuildCityMove(vertex) =>
          ActionSpecification(BUILD_CITY, BuildCityPayload(BuildCity(vertex.proto)))
        case BuildRoadMove(edge) =>
          ActionSpecification(BUILD_ROAD, BuildRoadPayload(BuildRoad(edge.proto)))
        case PortTradeMove(give, get) =>
          ActionSpecification(PORT_TRADE, PortTradePayload(PortTrade(give.proto, get.proto)))
        case KnightMove(MoveRobberAndStealMove(node, playerStole)) =>
          ActionSpecification(PLAY_KNIGHT, PlayKnightPayload(MoveRobberAndSteal(Vertex(node).proto, playerStole)))
        case YearOfPlentyMove(res1, res2) =>
          ActionSpecification(PLAY_YEAR_OF_PLENTY, PlayYearOfPlentyPayload(PlayYearOfPlenty(res1.proto, res2.proto)))
        case MonopolyMove(res) =>
          ActionSpecification(PLAY_MONOPOLY, PlayMonopolyPayload(PlayMonopoly(res.proto)))
        case RoadBuilderMove(road1, road2) =>
          ActionSpecification(PLAY_ROAD_BUILDER, PlayRoadBuilderPayload(PlayRoadBuilder(road1.proto, road2.map(_.proto))))
      }
  }

  implicit val actionFromProto: ProtoCoder[ActionSpecification, CatanMove] = pMove => {
    import Specification._
    pMove match {
      case ActionSpecification(ROLL_DICE, _) => RollDiceMove
      case ActionSpecification(END_TURN, _) => EndTurnMove
      case ActionSpecification(INITIAL_PLACEMENT, InitialPlacementPayload(InitialPlacement(settlement, road, first))) =>
        InitialPlacementMove(first, settlement.proto, road.proto)
      case ActionSpecification(DISCARD_RESOURCES, DiscardResourcesPayload(DiscardResources(resources))) =>
        DiscardResourcesMove(resources.proto)
      case ActionSpecification(MOVE_ROBBER_AND_STEAL, MoveRobberAndStealPayload(MoveRobberAndSteal(vertex, playerStole))) =>
        MoveRobberAndStealMove(vertex.proto.node, playerStole)
      case ActionSpecification(BUY_DEVELOPMENT_CARD, _) => BuyDevelopmentCardMove
      case ActionSpecification(BUILD_SETTLEMENT, BuildSettlementPayload(BuildSettlement(vertex))) =>
        BuildSettlementMove(vertex.proto)
      case ActionSpecification(BUILD_CITY, BuildCityPayload(BuildCity(vertex))) =>
        BuildCityMove(vertex.proto)
      case ActionSpecification(BUILD_ROAD, BuildRoadPayload(BuildRoad(edge))) =>
        BuildRoadMove(edge.proto)
      case ActionSpecification(PORT_TRADE, PortTradePayload(PortTrade(give, get))) =>
        PortTradeMove(give.proto, get.proto)
      case ActionSpecification(PLAY_KNIGHT, PlayKnightPayload(MoveRobberAndSteal(vertex, playerStole))) =>
        KnightMove(MoveRobberAndStealMove(vertex.proto.node, playerStole))
      case ActionSpecification(PLAY_YEAR_OF_PLENTY, PlayYearOfPlentyPayload(PlayYearOfPlenty(res1, res2))) =>
        YearOfPlentyMove(res1.proto, res2.proto)
      case ActionSpecification(PLAY_MONOPOLY, PlayMonopolyPayload(PlayMonopoly(res))) =>
        MonopolyMove(res.proto)
      case ActionSpecification(PLAY_ROAD_BUILDER, PlayRoadBuilderPayload(PlayRoadBuilder(road1, road2))) =>
        RoadBuilderMove(road1.proto, road2.map(_.proto))
    }
  }

  implicit def protoResult[U <: MoveResult]: ProtoCoder[(U,  List[SOCTransactions]), ActionResult] = { case (move: U, transactions: List[SOCTransactions]) =>
    import Result._

    import protocoder.implicits.ResourceProto.{protoTransactions => transactionImplicit}
    val protoTransactions: Seq[ResourceTransaction] = transactions.map( t => ProtoCoder.ops.proto(t)(transactionImplicit))
      move match {
        case RollResult(roll) =>
          ActionResult.of(ROLL_DICE, protoTransactions, RollDicePayload(RollDiceResult(roll.number)))
        case EndTurnMove => ActionResult(END_TURN)
        case InitialPlacementMove(first, settlement, road) =>
          ActionResult(INITIAL_PLACEMENT, protoTransactions, InitialPlacementPayload(InitialPlacement(settlement.proto, road.proto, first)))
        case DiscardResourcesResult(_) =>
          ActionResult.of(DISCARD_RESOURCES, protoTransactions, Empty)
        case MoveRobberAndStealResult(viewableBy, node, steal) =>
          val t = steal.map { s =>
            val p = protoTransactions.head.player
            val o = protoTransactions.head.getSteal.otherPlayer
           s.res.fold(Seq(ResourceTransaction(p, ResourceTransaction.Transaction.Steal(ResourceTransaction.Steal(o, Seq(ResourceCount(UNKNOWN_RESOURCE, 1))))))) { _ =>
             protoTransactions
           }
         }.getOrElse(Nil)

          ActionResult.of(MOVE_ROBBER_AND_STEAL, t, MoveRobberAndStealPayload(MoveRobberAndSteal(Vertex(node).proto, steal.map(_.player))))
//          val result = steal.map { p =>
//                      ActionResult(hiddenCard = Some(HiddenCard(viewableBy, p.res.fold[HiddenCard.Card](HiddenCard.Card.Empty)(r => ResourceCard(r.proto)))))
//                    }
//                    val playerStole = steal.map(_.player)
//                    ActionResult(MOVE_ROBBER_AND_STEAL, result, MoveRobberAndStealPayload(MoveRobberAndSteal(Vertex(node).proto, playerStole)))
        case BuyDevelopmentCardResult(viewableBy, card) =>
          ActionResult.of(BUY_DEVELOPMENT_CARD, protoTransactions, card.fold[Result](Empty)(c => BuyDevelopmentCardPayload(PBuyDevResult(c.proto))))
        case BuildSettlementMove(vertex) =>
          ActionResult(BUILD_SETTLEMENT, protoTransactions, BuildSettlementPayload(BuildSettlement(vertex.proto)))
        case BuildCityMove(vertex) =>
          ActionResult(BUILD_CITY, protoTransactions, BuildCityPayload(BuildCity(vertex.proto)))
        case BuildRoadMove(edge) =>
          ActionResult(BUILD_ROAD, protoTransactions, BuildRoadPayload(BuildRoad(edge.proto)))
        case PortTradeMove(_, _) =>
          ActionResult(PORT_TRADE, protoTransactions, Empty)
        case KnightResult( MoveRobberAndStealResult(viewableBy, node, steal)) =>
          val t = steal.map { s =>
            val p = protoTransactions.head.player
            val o = protoTransactions.head.getSteal.otherPlayer
            s.res.fold(Seq(ResourceTransaction(p, ResourceTransaction.Transaction.Steal(ResourceTransaction.Steal(o, Seq(ResourceCount(UNKNOWN_RESOURCE, 1))))))) { _ =>
              protoTransactions
            }
          }.getOrElse(Nil)
          ActionResult.of(PLAY_KNIGHT, t, PlayKnightPayload(MoveRobberAndSteal(Vertex(node).proto, steal.map(_.player))))
        //            val result = steal.map { p =>
        //              ActionResult(hiddenCard = Some(HiddenCard(viewableBy, p.res.fold[HiddenCard.Card](HiddenCard.Card.Empty)(r => ResourceCard(r.proto)))))
        //            }
        //            val playerStole = steal.map(_.player)
        //            ActionResult(PLAY_KNIGHT, result, PlayKnightPayload(MoveRobberAndSteal(Vertex(node).proto, playerStole)))
        case YearOfPlentyMove(_, _) =>
          ActionResult(PLAY_YEAR_OF_PLENTY, protoTransactions, Empty)
        case MonopolyResult(_) =>
          ActionResult.of(PLAY_MONOPOLY, protoTransactions, Empty)
        //            val result = Some(ActionResult(resourceTransactions = cardsLost.view.mapValues(r => ResourceTransaction(LOSE, r.proto)).toMap))
        //            ActionResult(PLAY_MONOPOLY, result)
        case RoadBuilderMove(road1, road2) =>
          ActionResult(PLAY_ROAD_BUILDER, protoTransactions, PlayRoadBuilderPayload(PlayRoadBuilder(road1.proto, road2.map(_.proto))))
      }
  }

//  implicit val resultFromProto: ProtoCoder[GameEvent, MoveResult] = pMove => pMove match {
//    // public actions
//    case GameEvent(ROLL_DICE, Some(ActionResult(Some(number), None, _)), _) => RollResult(Roll(number))
//    case GameEvent(END_TURN, _, _) => EndTurnMove
//    case GameEvent(INITIAL_PLACEMENT, None, InitialPlacementPayload(InitialPlacement(settlement, road, first))) =>
//      InitialPlacementMove(first, settlement.proto, road.proto)
//    case GameEvent(DISCARD_RESOURCES, Some(ActionResult(None, None, discards)), _) =>
//      DiscardResourcesResult(discards.view.mapValues { case ResourceTransaction(ResourceTransaction.TransactionType.LOSE, res) => res.proto }.toMap)
//    case GameEvent(BUILD_SETTLEMENT, None, BuildSettlementPayload(BuildSettlement(vertex))) =>
//      BuildSettlementMove(vertex.proto)
//    case GameEvent(BUILD_CITY, None, BuildCityPayload(BuildCity(vertex))) =>
//      BuildCityMove(vertex.proto)
//    case GameEvent(BUILD_ROAD, None, BuildRoadPayload(BuildRoad(edge))) =>
//      BuildRoadMove(edge.proto)
//    case GameEvent(PORT_TRADE, None, PortTradePayload(PortTrade(give, get))) =>
//      PortTradeMove(give.proto, get.proto)
//    case GameEvent(PLAY_YEAR_OF_PLENTY, None, PlayYearOfPlentyPayload(PlayYearOfPlenty(res1, res2))) =>
//      YearOfPlentyMove(res1.proto, res2.proto)
//    case GameEvent(PLAY_ROAD_BUILDER, None, PlayRoadBuilderPayload(PlayRoadBuilder(road1, road2))) =>
//      RoadBuilderMove(road1.proto, road2.map(_.proto))
//    case GameEvent(PLAY_MONOPOLY, Some(ActionResult(None, None, cardsLost)), PlayMonopolyPayload(PlayMonopoly(res))) =>
//      MonopolyResult(cardsLost.view.mapValues { case ResourceTransaction(ResourceTransaction.TransactionType.LOSE, res) => res.proto }.toMap)
//
//    // actions with private info
//
//    // No card was stolen
//    case GameEvent(MOVE_ROBBER_AND_STEAL, None, MoveRobberAndStealPayload(MoveRobberAndSteal(vertex, None))) =>
//      MoveRobberAndStealResult(Nil, vertex.proto.node, None)
//    // Card was stolen but card is not viewable
//    case GameEvent(MOVE_ROBBER_AND_STEAL, Some(ActionResult(None, Some(HiddenCard(viewableBy, HiddenCard.Card.Empty)), _)), MoveRobberAndStealPayload(MoveRobberAndSteal(vertex, Some(playerStole)))) =>
//      MoveRobberAndStealResult(viewableBy, vertex.proto.node, Some(RobPlayer(playerStole, None)))
//    // Card was stolen and card is viewable
//    case GameEvent(MOVE_ROBBER_AND_STEAL, Some(ActionResult(None, Some(HiddenCard(viewableBy, ResourceCard(res))), _)), MoveRobberAndStealPayload(MoveRobberAndSteal(vertex, Some(playerStole)))) =>
//      MoveRobberAndStealResult(viewableBy, vertex.proto.node, Some(RobPlayer(playerStole, Some(res.proto))))
//
//    // No card was stolen
//    case GameEvent(PLAY_KNIGHT, None, PlayKnightPayload(MoveRobberAndSteal(vertex, None))) =>
//      KnightResult(MoveRobberAndStealResult(Nil, vertex.proto.node, None))
//    // Card was stolen but card is not viewable
//    case GameEvent(PLAY_KNIGHT, Some(ActionResult(None, Some(HiddenCard(viewableBy, HiddenCard.Card.Empty)), _)), PlayKnightPayload(MoveRobberAndSteal(vertex, Some(playerStole)))) =>
//      KnightResult(MoveRobberAndStealResult(viewableBy, vertex.proto.node, Some(RobPlayer(playerStole, None))))
//    // Card was stolen and card is viewable
//    case GameEvent(PLAY_KNIGHT, Some(ActionResult(None, Some(HiddenCard(viewableBy, ResourceCard(res))), _)), PlayKnightPayload(MoveRobberAndSteal(vertex, Some(playerStole)))) =>
//      KnightResult(MoveRobberAndStealResult(viewableBy, vertex.proto.node, Some(RobPlayer(playerStole, Some(res.proto)))))
//
//    // No development cards left in deck
//    case GameEvent(BUY_DEVELOPMENT_CARD, Some(ActionResult(None, None, _)), _) =>
//      BuyDevelopmentCardResult(Nil, None)
//    // development card was bought but card is not viewable
//    case GameEvent(BUY_DEVELOPMENT_CARD, Some(ActionResult(None, Some(HiddenCard(viewableBy, HiddenCard.Card.Empty)),_)), _) =>
//      BuyDevelopmentCardResult(viewableBy, None)
//    case GameEvent(BUY_DEVELOPMENT_CARD, Some(ActionResult(None, Some(HiddenCard(viewableBy, DevelopmentCard(dev))), _)), _) =>
//      BuyDevelopmentCardResult(viewableBy, Some(dev.proto))
//  }

}
