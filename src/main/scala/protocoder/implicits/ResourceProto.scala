package protocoder.implicits

import protos.soc.inventory.PossibleHands.HandCombination
import protos.soc.inventory.PossibleHands.HandCombination.HandsWithMultiplier
import protos.soc.inventory.ProbableResourceSet.ProbableCardValue
import protos.soc.inventory.ResourceCount
import soc.inventory.Inventory.{PerfectInfo, PublicInfo}
import soc.inventory.developmentCard.{DevelopmentCardSpecification, DevelopmentCardSpecificationSet}
import soc.inventory.resources.CatanResourceSet.Resources
import soc.inventory.resources.{CatanResourceSet, PossibleHands, ProbableResourceSet}
import soc.inventory._
import util.MapReverse
import protocoder.ProtoCoder
import protocoder.ProtoCoder.ops._
import protos.soc.inventory.{DevelopmentCard => PDev, DevelopmentCardSpecification => PDCS, PossibleHands => PPossible, PrivateInventory => PPrivate, ProbableResourceSet => PProb, PublicInventory => PPublic, Resource => PResource, Inventory => PInventory}

object ResourceProto {

  private lazy val resourceMap: Map[Resource, PResource] = Map(Brick -> PResource.BRICK, Ore -> PResource.ORE, Sheep -> PResource.SHEEP, Wheat -> PResource.WHEAT, Wood -> PResource.WOOD)
  private lazy val reverseResourceMap = MapReverse.reverseMap(resourceMap)
  private lazy val devCardMap: Map[DevelopmentCard, PDev] = Map(Knight -> PDev.KNIGHT, YearOfPlenty -> PDev.YEAR_OF_PLENTY, Monopoly -> PDev.MONOPOLY, RoadBuilder -> PDev.ROAD_BUILDER, CatanPoint -> PDev.VICTORY_POINT)
  private lazy val reverseDevCardMap = MapReverse.reverseMap(devCardMap)

  implicit val protoResource: ProtoCoder[Resource, PResource] = res => resourceMap(res)
  implicit val resourceFromProto: ProtoCoder[PResource, Resource] = protoRes => reverseResourceMap(protoRes)
  implicit val protoDevelopmentCard: ProtoCoder[DevelopmentCard, PDev] = dev => devCardMap(dev)
  implicit val developmentCardFromProto: ProtoCoder[PDev, DevelopmentCard] = protoDev => reverseDevCardMap(protoDev)

  implicit val protoResourceSet: ProtoCoder[CatanResourceSet[Int], Seq[ResourceCount]] = resources => resources.amountMap.toSeq.map { case (res, amt) => ResourceCount(res.proto, amt) }
  implicit val resourceSetFromProto: ProtoCoder[Seq[ResourceCount], CatanResourceSet[Int]] = { protoResources =>
     CatanResourceSet.fromMap(protoResources.map { case ResourceCount(res, amt) => res.proto -> amt}.toMap[Resource, Int])
  }

  implicit val protoDevSpecification: ProtoCoder[DevelopmentCardSpecification, PDCS] = { d =>
    PDCS(d.`type`.proto, d.turnPurchased, d.turnPlayed)
  }

  implicit val devSpecificationFromProto: ProtoCoder[PDCS, DevelopmentCardSpecification] = { d =>
    DevelopmentCardSpecification(d.`type`.proto, d.turnPurchased, d.turnPlayed)
  }

  implicit val protoProbableResourceSet: ProtoCoder[ProbableResourceSet, PProb] = { probableResourceSet =>
    PProb(Resource.list.filter(probableResourceSet.contains).map { res =>
      ProbableCardValue(res.proto, probableResourceSet.getKnownAmount(res), probableResourceSet.getUnknownAmount(res))
    })
  }

  implicit val probableResourceSetFromProto: ProtoCoder[PProb, ProbableResourceSet] = { protoProb =>
    val (known, unknown) = protoProb.probableResourceCards.foldLeft((CatanResourceSet.empty[Int], CatanResourceSet.empty[Double])) { case ((k, u), card) =>
      val res = card.`type`.proto
      (k.add(card.knownAmount, res), u.add(card.unknownAmount, res))
    }
    ProbableResourceSet(known, unknown)
  }

  implicit val protoPossibleHands: ProtoCoder[PossibleHands, PPossible] = { possibleHands =>
    implicit val protoHandCombination: ProtoCoder[Map[Int, (Resources, Int)], HandCombination] = { mapMult =>
      HandCombination(mapMult.view.mapValues { case (res, mult) => HandsWithMultiplier(res.proto, mult) }.toMap)
    }
    PPossible(possibleHands.hands.map(_.proto))
  }

  implicit val possibleHandsFromProto: ProtoCoder[PPossible, PossibleHands] = { protoPossible =>
    PossibleHands(protoPossible.hands.map {
      _.hand.view.mapValues { case HandsWithMultiplier(cards, mult) => (cards.proto, mult) }.toMap
    })
  }

  implicit val protoPublicInventory: ProtoCoder[PublicInfo, PPublic] = { inv =>
    PPublic(
      inv.numCards,
      inv.numUnplayedDevCards,
      inv.playedDevCards.cards.map(_.proto))
  }

  implicit val publicInventoryFromProto: ProtoCoder[PPublic, PublicInfo] = { protoInv =>
    PublicInfoInventory(
      DevelopmentCardSpecificationSet( protoInv.playedDevelopmentCards.map(_.proto).toList),
      protoInv.cardCount,
      protoInv.developmentCardCount
    )
  }

  implicit val protoPrivateInventory: ProtoCoder[PerfectInfo, PPrivate] = { inv =>
    PPrivate(
      inv.resourceSet.proto,
      inv.developmentCards.filterUnPlayed.cards.map(_.proto))
  }

  implicit val privateInventoryFromProto: ProtoCoder[PPrivate, PerfectInfo] = { protoInv =>
    PerfectInfoInventory(
      protoInv.resourceCards.proto,
      DevelopmentCardSpecificationSet(protoInv.unplayedDevelopmentCards.map(_.proto).toList)
    )
  }

  implicit def protoInventory[T <: Inventory[T]]: ProtoCoder[T, PInventory] = { inv =>
    inv match {
      case publicInfo: PublicInfo =>
        PInventory.of(PInventory.Inventory.Public(protoPublicInventory.proto(publicInfo)))
      case privateInfo: PerfectInfo =>
        PInventory.of(PInventory.Inventory.Private(protoPrivateInventory.proto(privateInfo)))
    }
  }

//  implicit val protoDevCardSet: ProtoCoder[DevelopmentCardSet[Int], PDevSet] = dev => PDevSet(CatanSet.toList(dev).map(_.proto))
//  implicit val devCardFromProto: ProtoCoder[PDevSet, DevelopmentCardSet[Int]] = { protoDevCards =>
//    import DevelopmentCardSet._
//    CatanSet.fromList[DevelopmentCard, DevelopmentCardSet[Int]](protoDevCards.developmentCards.map(_.proto))
//  }
//  implicit val protoProbableInventory: ProtoCoder[ProbableInfo, PProbable] = inv => PProbable(inv.probableResourceSet.proto, null)
//  implicit val protoPerfectInventory: ProtoCoder[PerfectInfo, PPerfect] = { inv =>
//    PPerfect(
//      inv.resourceSet.proto,
//      inv.playedDevCards.proto,
//      inv.canPlayDevCards.proto,
//      inv.cannotPlayDevCards.proto
//    )
//  }
//
//  implicit val perfectInventoryFromProto: ProtoCoder[PPerfect, PerfectInfo] = { protoInv =>
//    import DevelopmentCardSet._
//    import CatanResourceSet._
//    PerfectInfoInventory(
//      CatanSet.fromList[Resource, CatanResourceSet[Int]](protoInv.resourceCards.resourceCards.map(_.proto).toList),
//      CatanSet.fromList[DevelopmentCard, DevelopmentCardSet[Int]](protoInv.playedDevCards.developmentCards.map(_.proto).toList),
//      CatanSet.fromList[DevelopmentCard, DevelopmentCardSet[Int]](protoInv.canPlayDevCards.developmentCards.map(_.proto).toList),
//      CatanSet.fromList[DevelopmentCard, DevelopmentCardSet[Int]](protoInv.cannotPlayDevCards.developmentCards.map(_.proto).toList)
//    )
//  }
//}
}
