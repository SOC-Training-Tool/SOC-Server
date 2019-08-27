package soc.akka

import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.messages.{GameMessage, Terminate}
import soc.game.dice.NormalDice
import soc.game.player.moveSelector.PossibleMoveSelector
import soc.game._
import soc.game.board.{BaseBoardConfiguration, BaseCatanBoard}
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo}
import soc.game.inventory._
import soc.storage.aws.AWSMoveSaver

import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import soc.aws.client.CatanGameStoreClientFactory
import soc.game.inventory.resources.CatanResourceSet

object Main extends App {

  implicit val random = new Random()

  val boardConfig = BaseCatanBoard.randomBoard

  val dice = NormalDice()

  import InventoryManager._
  import BaseCatanBoard._
  implicit val gameRules = GameRules()

  val dCardDeck: List[DevelopmentCard] = DevelopmentCardDeckBuilder.buildDeckByCardTypeAndAmount(
    Map(Knight -> Knight.initAmount,
      CatanPoint -> CatanPoint.initAmount,
      RoadBuilder -> RoadBuilder.initAmount,
      Monopoly -> Monopoly.initAmount,
      YearOfPlenty -> YearOfPlenty.initAmount)
  )

  import CatanResourceSet._
  import io.circe.generic.auto._
  val awsGameSaver = new AWSMoveSaver[BaseBoardConfiguration](CatanGameStoreClientFactory.createClient())
  val moveSaverActor: ActorRef[GameMessage] = ActorSystem(MoveSaverBehavior.moveSaverBehavior(awsGameSaver), "moveSaver")

  val randSelector = PossibleMoveSelector.randSelector[NoInfo]

  val players = Map(
    ("randomPlayer", 0) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player0"),
    ("randomPlayer", 1) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player1"),
    ("randomPlayer", 2) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player2"),
    ("randomPlayer", 3) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player3")
  )

  val randomMoveResultProvider = new RandomMoveResultProvider(dice, dCardDeck)

  val numGames = 100
  val averageGameLengthSeconds = 20

  val games = for {
    i <- 1 to numGames
    config = GameConfiguration[PerfectInfo, NoInfo, BaseBoardConfiguration](i, boardConfig, players, randomMoveResultProvider, Some(moveSaverActor), gameRules)
  } yield ActorSystem(GameBehavior.gameBehavior(config), s"SettlersOfCatan$i").whenTerminated

  Await.result(Future.sequence(games), (numGames * averageGameLengthSeconds).seconds)

  println("gamesAreOver")

  //println(inMemoryGameSaver.moves.groupBy(_.gameId).map(_._2.length).sum.toDouble / 100.0)

  players.values.foreach(_ ! Terminate)
  moveSaverActor ! Terminate
}
