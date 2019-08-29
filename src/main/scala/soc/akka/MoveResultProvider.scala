package soc.akka

import akka.actor.typed.scaladsl.Behaviors
import akka.pattern.pipe
import soc.akka.messages.{GameMessage, MoveResultProviderMessage, Terminate}
import soc.game.{GameState, Roll}
import soc.game.dice.Dice
import soc.game.inventory.Inventory.PerfectInfo
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory.resources.CatanResourceSet.Resources
import soc.game.inventory.{CatanSet, DevelopmentCard, Inventory, Resource}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

//object MoveResultProvider {
//
//  def moveResultProvider[GAME <: Inventory](provider: MoveResultProvider[GAME])(implicit ec: ExecutionContext) = Behaviors.receiveMessage[MoveResultProviderMessage[GAME]] {
//
//    case MoveResultProviderMessage(_, _, RollDiceMove, respondTo) =>
//      provider.rollDiceMoveResult.map(respondTo ! _)
//      Behaviors.same
//
//    case MoveResultProviderMessage(gs: GameState[GAME], _, move @ MoveRobberAndStealMove(_, playerStole), respondTo) =>
//      provider.stealCardMoveResult(gs, playerStole).map(roll => respondTo ! )
//      Behaviors.same
//
//    case MoveResultProviderMessage(gs: GameState[GAME], _, move @ BuyDevelopmentCardMove, respondTo) =>
//      provider.buyDevelopmentCardMoveResult(gs).map(roll => respondTo ! )
//      Behaviors.same
//
//    case MoveResultProviderMessage(gs:GameState[GAME], _, move @ MonopolyMove(res), respondTo) =>
//      provider.playMonopolyMoveResult(gs, res).map(roll => respondTo ! )
//      Behaviors.same
//  }
//}

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
    dcardDeck.drop(dcardDeck.length - gs.devCardsDeck).headOption
  }

  override def playMonopolyMoveResult(gs: GameState[PerfectInfo], res: Resource): Future[Map[Int, Resources]] = Future.successful {
    gs.players.getPlayers.map { p =>
      p.position -> CatanResourceSet.empty[Int].add(p.inventory.resourceSet.getAmount(res), res)
    }.toMap
  }
}