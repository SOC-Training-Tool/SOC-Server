package soc.robot

import soc.game.{CombinationMapIterator, GameState, Roll}
import soc.game.inventory.Inventory.ProbableInfo
import soc.game.inventory.resources.CatanResourceSet.Resources
import soc.game.inventory.{CatanSet, DevelopmentCard, InventoryManager, ProbableInfoInventoryManager, Resource}
import soc.game.inventory.resources.{CatanResourceSet, PossibleHands, ProbableResourceSet, Steal}
import soc.game.moves.{BuyDevelopmentCardMove, BuyDevelopmentCardResult, CatanMove, CatanPossibleMoves, DiscardResourcesMove, KnightMove, KnightResult, MonopolyMove, MonopolyResult, MoveResult, MoveRobberAndStealMove, MoveRobberAndStealResult, RollDiceMove, RollResult}
import soc.game.moves.moveSelector.PossibleMoveSelector


object RobotMoveSelector {

  implicit def toProbableInventoryManager(invManager: InventoryManager[ProbableInfo]): ProbableInfoInventoryManager = invManager.asInstanceOf[ProbableInfoInventoryManager]

  def stateEvaluatorByMax(playerId: Int, eval: GameState[ProbableInfo] => Double) = PossibleMoveSelector[ProbableInfo] { case (state, currPlayer, moves) => {

    def resourcesToDiscards(set: Resources, mult: Int): Iterator[Resources] = {
      var discardIterator = CatanSet.toList(set).combinations(set.getTotal / 2).map(res => CatanResourceSet.apply(res: _*))
      (1 to mult).flatMap { _ =>
        val (a, b) = discardIterator.duplicate
        discardIterator = b
        a
      }.toIterator
    }

    def allPossibleRobberMovementAndSteal(gs: GameState[ProbableInfo], robber: Int) = {
      CatanPossibleMoves.getPossibleRobberLocations(gs, robber).flatMap {
        case MoveRobberAndStealMove(loc, None) => Iterator(SimpleProbState(gs.apply(robber, MoveRobberAndStealResult(loc, None))))
        case MoveRobberAndStealMove(loc, Some(playerStole)) =>
          val probableHand: ProbableResourceSet = gs.players.getPlayer(playerStole).inventory.probableResourceSet
          Resource.list.filter(probableHand.mightContain).toIterator.map { res =>
            StateProbState(probableHand.getProbabilityOfResourceInHand(res), SimpleProbState(gs.apply(robber, MoveRobberAndStealResult(loc, Some(Steal(robber, playerStole, Some(CatanResourceSet.empty[Int].add(1, res))))))))
          }
      }
    }.toIterator

    lazy val possibleDiscardsForNonPlayer = {
      val possibleHands: PossibleHands = state.players.inventoryManager.possibleHands
      val toDiscard = state.players.getPlayers.filter(_.numCards > state.players.gameRules.discardCards).map(_.position).filterNot(_ == playerId)
      possibleHands.handsForPlayers.filterKeys(toDiscard.contains).mapValues {
        _.toIterator.flatMap { case (set, mult) => resourcesToDiscards(set, mult) }
      }
    }

    lazy val allPossibleDiscards: Map[Int, Iterator[Resources]] = {
      if (state.players.getPlayer(playerId).numCards <= state.players.gameRules.discardCards) possibleDiscardsForNonPlayer
      else {
        possibleDiscardsForNonPlayer + (currPlayer -> {
          val possibleHands = state.players.inventoryManager.possibleHands.handsForPlayers(playerId)
          possibleHands.flatMap { case (set, mult) => resourcesToDiscards(set, mult) }.toIterator
        })
      }
    }

    moves.map {
      case RollDiceMove =>
        val probRolls: Iterator[ProbState] = (2 to 12).map { n =>
          val roll = Roll(n)
          StateProbState(roll.prob, SimpleProbState(state.apply(currPlayer, RollResult(roll))))
        }.toIterator
        val result7: ProbState = {
          UnifiedProbState(CombinationMapIterator.getIterator(allPossibleDiscards).map(d =>
            state(currPlayer, RollResult(Roll(7))).apply(currPlayer, DiscardResourcesMove(d))
          ).flatMap { gs => allPossibleRobberMovementAndSteal(gs, playerId) })
        }
        (NonUnifiedProbState(probRolls ++ Iterator(StateProbState(Roll(7).prob, result7))).eval(eval), RollDiceMove)

      case m@DiscardResourcesMove(setMap) =>
        val resultDiscard = UnifiedProbState(
          CombinationMapIterator.getIterator(possibleDiscardsForNonPlayer + (playerId -> Iterator(setMap(playerId)))).map(d =>
            state(currPlayer, RollResult(Roll(7))).apply(currPlayer, DiscardResourcesMove(d))
          ).flatMap { gs => allPossibleRobberMovementAndSteal(gs, playerId) })
        (resultDiscard.eval(eval), m)

      case m@MoveRobberAndStealMove(node, None) =>
        (eval(state.apply(playerId, MoveRobberAndStealResult(node, None))), m)
      case m@MoveRobberAndStealMove(node, Some(victim)) =>
        val probableHand: ProbableResourceSet = state.players.getPlayer(victim).inventory.probableResourceSet
        val prob = UnifiedProbState(Resource.list.filter(probableHand.mightContain).toIterator.map { res =>
          StateProbState(probableHand.getProbabilityOfResourceInHand(res), SimpleProbState(state.apply(playerId, MoveRobberAndStealResult(node, Some(Steal(playerId, victim, Some(CatanResourceSet.empty[Int].add(1, res))))))))
        })
        (prob.eval(eval), m)

      case BuyDevelopmentCardMove =>
        val possibleCard = state.players.inventoryManager.possibleDevCards.prob
        val buyResults = NonUnifiedProbState(DevelopmentCard.list.toIterator.filter(possibleCard.contains).map { card =>
          StateProbState(possibleCard.getAmount(card), SimpleProbState(state.apply(currPlayer, BuyDevelopmentCardResult(Some(card)))))
        })
        (buyResults.eval(eval), BuyDevelopmentCardMove)

      case m@KnightMove(MoveRobberAndStealMove(node, None)) =>
        (eval(state.apply(playerId, KnightResult(MoveRobberAndStealResult(node, None)))), m)
      case m@KnightMove(MoveRobberAndStealMove(node, Some(victim))) =>
        val probableHand: ProbableResourceSet = state.players.getPlayer(victim).inventory.probableResourceSet
        val prob = NonUnifiedProbState(Resource.list.filter(probableHand.mightContain).toIterator.map { res =>
          StateProbState(probableHand.getProbabilityOfResourceInHand(res), SimpleProbState(state.apply(playerId, KnightResult(MoveRobberAndStealResult(node, Some(Steal(playerId, victim, Some(CatanResourceSet.empty[Int].add(1, res)))))))))
        })
        (prob.eval(eval), m)

      case m@MonopolyMove(res) =>
        val possibleHands: PossibleHands = state.players.inventoryManager.possibleHands
        val monoResult = UnifiedProbState(possibleHands.hands.map(_.flatMap{case (p, (set, mult)) =>
          (1 to mult).map { _ => p -> CatanResourceSet.empty[Int].add(set.getAmount(res), res) }
        }).map(MonopolyResult).toIterator.map(r => SimpleProbState(state.apply(playerId, r))))
        (monoResult.eval(eval), m)

      case move: CatanMove with MoveResult =>
        (eval(state.apply(playerId, move)), move)

    }.maxBy(_._1)._2
  }}
}

sealed trait ProbState {
  def eval(e: GameState[ProbableInfo] => Double): Double
}

case class UnifiedProbState(probStates: Iterator[ProbState]) extends ProbState {
  override def eval(e: GameState[ProbableInfo] => Double): Double = {
    val (a, b) = probStates.duplicate
    a.map(_.eval(e)).sum / b.length.toDouble
  }
}

case class NonUnifiedProbState(probStates: Iterator[ProbState]) extends ProbState {
  override def eval(e: GameState[ProbableInfo] => Double): Double = probStates.map(_.eval(e)).sum
}

case class StateProbState(probability: Double, probState: ProbState) extends ProbState {
  override def eval(e: GameState[ProbableInfo] => Double): Double = probability * probState.eval(e)
}

case class SimpleProbState(state: GameState[ProbableInfo]) extends ProbState {
  override def eval(e: GameState[ProbableInfo] => Double): Double = e(state)
}