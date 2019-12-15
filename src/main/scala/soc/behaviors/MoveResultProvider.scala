package soc.behaviors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import soc.behaviors.MoveResultProviderMessage.{GetMoveResultProviderMessage, MoveResultProviderMessage, SendMoveResultProviderMessage, StopResultProvider}
import soc.behaviors.messages.ResultResponse
import soc.core.{GameRules, Roll}
import soc.dice.{Dice, NormalDice}
import soc.inventory.Inventory.PerfectInfo
import soc.inventory.developmentCard.{DevelopmentCardManager, DevelopmentCardSet}
import soc.inventory.resources.CatanResourceSet.Resources
import soc.inventory.{CatanSet, DevelopmentCard, Inventory, Resource}
import soc.inventory.resources.{CatanResourceSet, Steal}
import soc.moves._
import soc.state.GameState

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success}

object MoveResultProvider {

  def moveResultProvider[GAME <: Inventory[GAME]](provider: MoveResultProvider[GAME])(implicit ec: ExecutionContext): Behavior[MoveResultProviderMessage[GAME]] = Behaviors.setup[MoveResultProviderMessage[GAME]] { context =>

    Behaviors.receiveMessage[MoveResultProviderMessage[GAME]] {

      case GetMoveResultProviderMessage(_, id, RollDiceMove, respondTo) =>
        context.pipeToSelf(provider.rollDiceMoveResult) {
          case Success(roll) => SendMoveResultProviderMessage[GAME](id, RollResult(roll), respondTo)
          case Failure(ex) => null
        }
        Behaviors.same

      case GetMoveResultProviderMessage(gs: GameState[GAME], id, MoveRobberAndStealMove(loc, playerStole), respondTo) =>
        context.pipeToSelf(provider.stealCardMoveResult(gs, playerStole)) {
          case Success(_) if playerStole.isEmpty => SendMoveResultProviderMessage[GAME](
            id,
            MoveRobberAndStealResult(Nil, loc, None),
            respondTo)
          case Success(steal) => SendMoveResultProviderMessage[GAME](
            id,
            MoveRobberAndStealResult(Seq(id, playerStole.get), loc, steal.flatMap(r => playerStole.map(v => RobPlayer(v, Some(r))))),
            respondTo)
          case Failure(ex) => null
        }
        Behaviors.same

      case GetMoveResultProviderMessage(gs: GameState[GAME], id, KnightMove(MoveRobberAndStealMove(loc, playerStole)), respondTo) =>
        context.pipeToSelf(provider.stealCardMoveResult(gs, playerStole)) {
          case Success(_) if playerStole.isEmpty => SendMoveResultProviderMessage[GAME](
            id,
            KnightResult(MoveRobberAndStealResult(Nil, loc, None)),
            respondTo)
          case Success(steal) => SendMoveResultProviderMessage[GAME](
            id,
            KnightResult(MoveRobberAndStealResult(Seq(id, playerStole.get), loc, steal.flatMap(r => playerStole.map(v => RobPlayer(v, Some(r)))))),
            respondTo)
          case Failure(ex) => null
        }
        Behaviors.same

      case GetMoveResultProviderMessage(gs: GameState[GAME], id, DiscardResourcesMove(resourceSet), respondTo) =>
        context.pipeToSelf(Future.successful(DiscardResourcesResult(Map(id -> resourceSet)))) {
          case Success(result) => SendMoveResultProviderMessage[GAME](id, result, respondTo)
          case Failure(ex) => null
        }
        Behaviors.same

      case GetMoveResultProviderMessage(gs: GameState[GAME], id, BuyDevelopmentCardMove, respondTo) =>
        context.pipeToSelf( provider.buyDevelopmentCardMoveResult(gs)) {
          case Success(card) => SendMoveResultProviderMessage[GAME](id, BuyDevelopmentCardResult(Seq(id), card), respondTo)
          case Failure(ex) => null
        }
        Behaviors.same

      case GetMoveResultProviderMessage(gs: GameState[GAME], id, MonopolyMove(res), respondTo) =>
        context.pipeToSelf( provider.playMonopolyMoveResult(gs, res)) {
          case Success(cardsLost) => SendMoveResultProviderMessage[GAME](id,MonopolyResult(cardsLost), respondTo)
          case Failure(ex) => null
        }
        Behaviors.same

      case GetMoveResultProviderMessage(_, id, move: CatanMove with MoveResult, respondTo) =>
        context.pipeToSelf(Future.successful(move)) {
          case Success(result) => SendMoveResultProviderMessage[GAME](id, result, respondTo)
          case Failure(ex) => null
        }
        Behaviors.same

      case SendMoveResultProviderMessage(id, result, respondTo) =>
        respondTo ! ResultResponse(id, result)
        Behaviors.same

      case StopResultProvider() =>
        Behaviors.stopped
    }
  }
}

object MoveResultProviderMessage {

  sealed trait MoveResultProviderMessage[GAME <: Inventory[GAME]]
  case class GetMoveResultProviderMessage[GAME <: Inventory[GAME]](gameState: GameState[GAME], id: Int, move: CatanMove, respondTo: ActorRef[ResultResponse]) extends MoveResultProviderMessage[GAME]
  case class SendMoveResultProviderMessage[GAME <: Inventory[GAME]](id: Int, move: MoveResult, respondTo: ActorRef[ResultResponse]) extends MoveResultProviderMessage[GAME]
  case class StopResultProvider[T <: Inventory[T]]() extends MoveResultProviderMessage[T]



}

trait MoveResultProvider[GAME <: Inventory[GAME]] {

  def rollDiceMoveResult: Future[Roll]

  def stealCardMoveResult(gs: GameState[GAME], playerStole: Option[Int]): Future[Option[Resource]]

  def buyDevelopmentCardMoveResult(gs:GameState[GAME]): Future[Option[DevelopmentCard]]

  def playMonopolyMoveResult(gs: GameState[GAME], res: Resource): Future[Map[Int, Resources]]

}

class RandomMoveResultProvider(dice: Dice, dcardDeck: List[DevelopmentCard])(implicit val rand: Random) extends MoveResultProvider[PerfectInfo] {
  override def rollDiceMoveResult: Future[Roll] = Future.successful {
    val (roll1, roll2) = dice.getRoll
    Roll(roll1 + roll2)
  }

  override def stealCardMoveResult(gs: GameState[PerfectInfo], playerStole: Option[Int]): Future[Option[Resource]] = Future.successful{
    playerStole.flatMap { victim =>
      val resources = gs.players.getPlayer(victim).inventory.resourceSet
      rand.shuffle(CatanSet.toList(resources)).headOption
    }
  }

  override def buyDevelopmentCardMoveResult(gs: GameState[PerfectInfo]): Future[Option[DevelopmentCard]] = Future.successful {
    dcardDeck.drop(dcardDeck.length - gs.developmentCardsLeft).headOption
  }

  override def playMonopolyMoveResult(gs: GameState[PerfectInfo], res: Resource): Future[Map[Int, Resources]] = Future.successful {
    gs.players.getPlayers.map { p =>
      p.position -> CatanResourceSet.empty[Int].add(p.inventory.resourceSet.getAmount(res), res)
    }.toMap
  }
}

object RandomMoveResultProvider {

  def apply(gameRules: GameRules)(implicit random: Random) = {
    val dice = NormalDice()
    val dCardDeck: List[DevelopmentCard] = (new DevelopmentCardManager(gameRules.initDevCardAmounts.kn, gameRules.initDevCardAmounts.pt, gameRules.initDevCardAmounts.mp, gameRules.initDevCardAmounts.rb, gameRules.initDevCardAmounts.yp)).buildDevelopmentCardDeck
    new RandomMoveResultProvider(dice, dCardDeck)
  }

}