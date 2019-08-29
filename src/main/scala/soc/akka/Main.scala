package soc.akka

import akka.actor.PoisonPill
import akka.actor.typed.{ActorRef, ActorSystem}
import soc.akka.messages.{GameMessage, Terminate}
import soc.game.dice.NormalDice
import soc.game.moves.moveSelector.PossibleMoveSelector
import soc.game._
import soc.game.board.{BaseBoardConfiguration, BaseCatanBoard}
import soc.game.inventory.Inventory.{NoInfo, PerfectInfo, ProbableInfo}
import soc.game.inventory._
import soc.storage.aws.AWSMoveSaver

import scala.concurrent.{Await, Future}
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import soc.aws.client.CatanGameStoreClientFactory
import soc.game.inventory.resources.CatanResourceSet
import soc.game.inventory.InventoryManager._
import soc.robot.RobotMoveSelector
import soc.simulation.SimulationQueue

object Main extends App {

  implicit val random = new Random()

  val boardConfig = BaseCatanBoard.randomBoard

  val dice = NormalDice()

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
  //val awsGameSaver = new AWSMoveSaver[BaseBoardConfiguration](CatanGameStoreClientFactory.createClient())
  //val moveSaverActor: ActorRef[GameMessage] = ActorSystem(MoveSaverBehavior.moveSaverBehavior(awsGameSaver), "moveSaver")

  val eval = (_: GameState[ProbableInfo]) => random.nextDouble()
  RobotMoveSelector.stateEvaluatorByMax(0, eval)

  //val randSelector = PossibleMoveSelector.randSelector[ProbableInfo]


  val players = Map(
    ("randomPlayer", 0) -> ActorSystem(PlayerBehavior.playerBehavior[PerfectInfo, NoInfo, ProbableInfo](RobotMoveSelector.stateEvaluatorByMax(0, eval)), "player0"),
    ("randomPlayer", 1) -> ActorSystem(PlayerBehavior.playerBehavior[PerfectInfo, NoInfo, ProbableInfo](RobotMoveSelector.stateEvaluatorByMax(1, eval)), "player1"),
    ("randomPlayer", 2) -> ActorSystem(PlayerBehavior.playerBehavior[PerfectInfo, NoInfo, ProbableInfo](RobotMoveSelector.stateEvaluatorByMax(2, eval)), "player2"),
    ("randomPlayer", 3) -> ActorSystem(PlayerBehavior.playerBehavior[PerfectInfo, NoInfo, ProbableInfo](RobotMoveSelector.stateEvaluatorByMax(3, eval)), "player3")
  )

  val randomMoveResultProvider = new RandomMoveResultProvider(dice, dCardDeck)

 SimulationQueue[PerfectInfo, NoInfo, BaseBoardConfiguration](players, randomMoveResultProvider, None, gameRules, 10, 100, 50).startGames
}
