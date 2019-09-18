package soc.akka

import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import soc.akka.MoveResultProviderMessage.StopResultProvider
import soc.akka.messages.{GameMessage, Terminate}
import soc.game.dice.NormalDice
import soc.game.player.moveSelector.PossibleMoveSelector
import soc.game._
import soc.game.board.{BaseBoardConfiguration, BaseCatanBoard}
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo, ProbableInfo}
import soc.game.inventory._
import soc.storage.aws.AWSMoveSaver

import scala.util.{Random, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import soc.aws.client.CatanGameStoreClientFactory
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory.InventoryHelper._
import soc.playerRepository.{PlayerContext, PlayerRepository}
import soc.simulation.{SimulationController}

import scala.concurrent.duration._

object Main extends App {

  implicit val random = new Random()

  val boardConfig = BaseCatanBoard.randomBoard

  val dice = NormalDice()

  import BaseCatanBoard._
  implicit val gameRules = GameRules.default

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

  val randSelector = PossibleMoveSelector.randSelector[ProbableInfo]

//  val players = Map(
//    ("randomPlayer", 0) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player0"),
//    ("randomPlayer", 1) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player1"),
//    ("randomPlayer", 2) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player2"),
//    ("randomPlayer", 3) -> ActorSystem(PlayerBehavior.playerBehavior(randSelector), "player3")
//  )

  val moveProvider = new RandomMoveResultProvider(dice, dCardDeck)
  val randomMoveResultProvider = ActorSystem(MoveResultProvider.moveResultProvider(moveProvider), "resultProvider")


 //SimulationQueue[PerfectInfo, NoInfo, BaseBoardConfiguration](players, randomMoveResultProvider, None, gameRules, 1, 60, 50).startGames

  val playerRepo = new PlayerRepository
  val simulationController = new SimulationController[PerfectInfo, NoInfo, BaseBoardConfiguration](
    playerRepo,
    randomMoveResultProvider,
    None,
    gameRules,
    5
  )

  playerRepo.addPlayer(new PlayerContext[PerfectInfo, ProbableInfo]("randomPlayer", randSelector))
  simulationController.queueGamesRandomBoard(
    "1",
    Map(0 -> "randomPlayer", 1 -> "randomPlayer", 2 -> "randomPlayer", 3 -> "randomPlayer"),
    10,
    10)

  val simulationFut = simulationController.run
  simulationController.terminateWhenFinished

  simulationFut.onComplete {
    case Success(_) =>
      moveSaverActor ! Terminate
      randomMoveResultProvider ! StopResultProvider()


  }




}
