package soc.game.resources

import soc.game.{CatanSet, Resource}
import soc.game.resources.CatanResourceSet._

import scala.annotation.tailrec

case class PossibleHands(hands: Seq[Map[Int, Resources]]) {

  lazy val handsForPlayers: Map[Int, Seq[Resources]] = hands.flatMap(_.keys).distinct.map {
    playerId => playerId -> hands.map(_ (playerId))
  }.toMap

  lazy val probableHands: Map[Int, ProbableResourceSet] = handsForPlayers.map { case (playerId, allResSets) =>
    val numHands = allResSets.length
//    val knownAmounts: Map[Resource, Int] = Resource.list.map (res => res -> allResSets.map(_.getAmount(res)).min).toMap
//    val knownSet: Resources = CatanResourceSet(knownAmounts)
//    val unkownSets: Seq[Resources] = allResSets.map(_.subtract(knownSet))
    val resMap: Map[Resource, (Int, Double)] = Resource.list.map { res =>
      val knownAmount = allResSets.map(_.getAmount(res)).min
      val unknownAmount = allResSets.map { set =>
        set.getAmount(res) - knownAmount
      }.sum.toDouble / numHands
      res -> (knownAmount, unknownAmount)
    }.toMap
    val knownMap: Map[Resource, Int] = resMap.mapValues(_._1)
    val unknownMap: Map[Resource, Double] = resMap.mapValues(_._2)
    val knownSet: Resources = CatanResourceSet(knownMap)
    val unknownSet: ResourceSet[Double] = CatanResourceSet(unknownMap)
    playerId -> ProbableResourceSet(knownSet, unknownSet)
  }

  def playerGainCards(player: Int, set: Resources): PossibleHands = copy {
    hands.headOption.fold(copy(Seq(Map(player -> CatanResourceSet.empty)))) {
      case hand if !hand.contains(player) => copy(hands.map(_ + (player -> CatanResourceSet.empty)))
      case _ => copy()
    }.hands.map { hand =>
      hand.map {
        case (`player`, resources) => player -> resources.add(set)
        case (p, rm) => p -> rm
      }
    }
  }

  def playerLoseCards(player: Int, set: Resources): PossibleHands = copy {
    hands.filter(_.get(player).fold(false)(_.contains(set))).map {
      _.map {
        case (`player`, resources) => player -> resources.subtract(set)
        case (p, rm) => p -> rm
      }
    }
  }

  def stealUnknownCards(robber: Int, victim: Int): PossibleHands = copy {
    hands.flatMap { hand =>
      Resource.list.filter(set => hand.get(victim).fold(false)(_.contains(set))).flatMap { res =>
        val set = CatanResourceSet(res)
        (1 to hand(victim).getAmount(res)).flatMap { _ =>
          PossibleHands(Seq(hand)).playerLoseCards(victim, set).playerGainCards(robber, set).hands
        }
      }
    }
  }

//  def playerMonopoly(player: Int, res: Resource): PossibleHands = copy {
//    hands.map { hand =>
//      val totalRes = hand.values.map(_.cards.getAmount(res)).sum
//      hand.map {
//        case (`player`, Hand(_, mult)) =>
//          player -> Hand(CatanResourceSet().add(totalRes, res), mult)
//        case (p, Hand(resources, mult)) =>
//          p -> Hand(resources.subtract(resources.getAmount(res), res), mult)
//      }
//    }
//  }

  @tailrec
  final def calculateHands(transactions: List[SOCTransactions]): PossibleHands = {
    if (transactions.isEmpty) copy()
    else calculateHands(transactions.head).calculateHands(transactions.tail)
  }

  def calculateHands(transaction: SOCTransactions): PossibleHands = transaction match {
    case Gain(player, set) => playerGainCards(player, set)
    case Lose(player, set) => playerLoseCards(player, set)
    case Steal(robber, victim, Some(set)) => playerLoseCards(victim, set).playerGainCards(robber, set)
    case Steal(robber, victim, None) => stealUnknownCards(robber, victim)
   // case MonopolyTransaction(player, resourceType) => playerMonopoly(player, resourceType)
  }

}

object SOCPossibleHands {

  def empty = PossibleHands(Nil)
}

sealed trait SOCTransactions

case class Gain(playerId: Int, resourceSet: Resources) extends SOCTransactions
case class Lose(playerId: Int, resourceSet: Resources) extends SOCTransactions
case class Steal(robber: Int, victim: Int, resourceSet: Option[Resources]) extends SOCTransactions
case class MonopolyTransaction(player: Int, resourceType: Resource) extends SOCTransactions



