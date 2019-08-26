package soc.game.inventory.resources

import CatanResourceSet._
import soc.game.inventory.Resource

import scala.annotation.tailrec

case class PossibleHands(hands: Seq[Map[Int, (Resources, Int)]]) {

  lazy val handsForPlayers: Map[Int, Seq[(Resources, Int)]] = hands.flatMap(_.keys).distinct.map {
    playerId => playerId -> hands.map(_ (playerId))
  }.toMap

  lazy val probableHands: Map[Int, ProbableResourceSet] = handsForPlayers.map { case (playerId, allResSets) =>
    val numHands = allResSets.map(_._2).sum
//    val knownAmounts: Map[Resource, Int] = Resource.list.map (res => res -> allResSets.map(_.getAmount(res)).min).toMap
//    val knownSet: Resources = CatanResourceSet(knownAmounts)
//    val unkownSets: Seq[Resources] = allResSets.map(_.subtract(knownSet))
    val resMap: Map[Resource, (Int, Double)] = Resource.list.map { res =>
      val knownAmount = allResSets.map(_._1.getAmount(res)).min
      val unknownAmount = allResSets.map { case (set, mult) =>
        (set.getAmount(res) - knownAmount) * mult
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
    hands.headOption.fold(copy(Seq(Map(player -> (CatanResourceSet.empty, 1))))) {
      case hand if !hand.contains(player) => copy(hands.map(_ + (player -> (CatanResourceSet.empty[Int], 1))))
      case _ => copy()
    }.hands.map { hand =>
      hand.map {
        case (`player`, (resources, mult)) => player -> (resources.add(set), mult)
        case (p, rm) => p -> rm
      }
    }
  }

  def playerLoseCards(player: Int, set: Resources): PossibleHands = copy {
    hands.filter(_.get(player).fold(false)(_._1.contains(set))).map {
      _.map {
        case (`player`, (resources, mult)) => player -> (resources.subtract(set), mult)
        case (p, rm) => p -> rm
      }
    }
  }

  def stealUnknownCards(robber: Int, victim: Int): PossibleHands = copy {
    hands.flatMap { hand =>
      Resource.list.filter(set => hand.get(victim).fold(false)(_._1.contains(set))).flatMap { res =>
        val set = CatanResourceSet(res)
        val amount = hand(victim)._1.getAmount(res)
        PossibleHands(Seq(hand)).playerLoseCards(victim, set).playerGainCards(robber, set).hands.map {
          _.map {
            case (`robber`, (resources, mult)) => robber -> (resources, mult * amount)
            case (`victim`, (resources, mult)) => victim -> (resources, mult * amount)
            case (p, rm) => p -> rm
          }
        }
      }
    }.groupBy(f => f).mapValues(_.length).map {
      case (m, 1) => m
      case (m, l) => m.mapValues { case (set, mult) => (set, mult * l)
      }
    }.toSeq
  }

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
  }
}

object SOCPossibleHands {

  def empty = PossibleHands(Nil)
}

sealed trait SOCTransactions

case class Gain(playerId: Int, resourceSet: Resources) extends SOCTransactions
case class Lose(playerId: Int, resourceSet: Resources) extends SOCTransactions
case class Steal(robber: Int, victim: Int, resourceSet: Option[Resources]) extends SOCTransactions
