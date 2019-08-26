//package soc.actors
//
//import akka.actor._
//import akka.testkit.{TestActorRef, TestKit, TestProbe}
//import log.Log
//import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}
//import soc.CatanFixtures
//import soc.actors.GameActor.{InitialPlacementRequest, InitialPlacementUpdate, PlayerAdded, StartingGame, TurnUpdate}
//import soc.actors.PlayerActor._
//import soc.game.{Misc, Port, ResourceHex, Roll, Wood}
//import soc.game.board.{CatanBoard, Edge, Vertex}
//import soc.game.dice.Dice
//import soc.game.messages._
//
//import scala.concurrent.duration._
//import scala.util.Random
//
//class GameActorSpec (_system: ActorSystem)
//  extends TestKit(_system)
//    with Matchers
//    with FunSpecLike
//    with BeforeAndAfterAll {
//
//  def this() = this(ActorSystem("GameActorSpec"))
//
//  private val vertexMap = Map(0 -> List(0, 1, 2, 3, 4, 5))
//  private def portMapFunc(ports: List[Port]) = Map(0 -> ports(0), 1 -> ports(0))
//  private val hexes = List(ResourceHex(Wood, Roll(6)))
//  private val ports = List(Misc)
//
//  implicit val rand = new Random()
//
//  val singleHexBoard = CatanBoard(vertexMap, portMapFunc, hexes, ports)
//
//  val stubbed6Dice = new Dice {
//    def getRoll = (3, 3)
//  }
//  val stubbed7Dice = new Dice {
//    def getRoll = (3, 4)
//  }
//
//  val stubbedLog = new Log {
//    override def print(message: String): Unit = ()
//  }
//
//  override def afterAll: Unit = {
//    shutdown(system)
//  }
//
//  def assertMessageSentToProbe(probe: TestProbe, message: Any) = {
//    probe.fishForMessage() {
//      case `message` => true
//      case _ => false
//    } shouldEqual message
//  }
//
//  describe("A Game Actor when receiving a") {
//
//    describe("AddPlayer message") {
//
//      it("should add player and send a PlayerAdded message to all players") {
//        val game = TestActorRef[GameActor](GameActor.props(singleHexBoard, stubbed6Dice, stubbedLog))
//        val probe = TestProbe()
//
//        game ! AddPlayer(0, "", probe.ref)
//        game.underlyingActor.playerActors.keys should contain only 0
//        assertMessageSentToProbe(probe, PlayerAdded(0))
//      }
//    }
//
//    describe("StartGame Message") {
//
//      it("should send a InitialPlacementRequest for first players starting settlement") {
//        val game = TestActorRef[GameActor](GameActor.props(singleHexBoard, stubbed6Dice, stubbedLog))
//        val probe = TestProbe()
//
//        game ! AddPlayer(0, "", probe.ref)
//        game ! StartGame
//
//        assertMessageSentToProbe(probe, StartingGame(singleHexBoard, Map(0 -> "")))
//        assertMessageSentToProbe(probe, InitialPlacementRequest(0, true))
//      }
//
//    }
//
//    describe("InitialPlacementResponse Message") {
//
//      it("from not-last player for first starting settlement should send an InitialPlacementUpdate " +
//        "message and InitialPlacementRequest message for first settlement for next player") {
//        val game = TestActorRef[GameActor](GameActor.props(singleHexBoard, stubbed6Dice, stubbedLog))
//        val firstPlayerProbe = TestProbe()
//        val secondPlayerProbe = TestProbe()
//
//        game ! AddPlayer(0, "", firstPlayerProbe.ref)
//        game ! AddPlayer(1, "", secondPlayerProbe.ref)
//        game ! StartGame
//
//        val settlement = Vertex(0)
//        val road = Edge(Vertex(0), Vertex(1))
//
//        game ! Response(0, InitialPlacementResponse(true, settlement, road))
//
//        assertMessageSentToProbe(firstPlayerProbe, InitialPlacementUpdate(0, true, settlement, road))
//        assertMessageSentToProbe(secondPlayerProbe, InitialPlacementUpdate(0, true, settlement, road))
//        assertMessageSentToProbe(firstPlayerProbe, InitialPlacementRequest(1, true))
//        assertMessageSentToProbe(secondPlayerProbe, InitialPlacementRequest(1, true))
//      }
//
//      it("from last player for first starting settlement should send an InitialPlacementUpdate " +
//        "message and InitialPlacementRequest message for second settlement for last player") {
//        val game = TestActorRef[GameActor](GameActor.props(singleHexBoard, stubbed6Dice, stubbedLog))
//        val firstPlayerProbe = TestProbe()
//        val secondPlayerProbe = TestProbe()
//
//        game ! AddPlayer(0, "", firstPlayerProbe.ref)
//        game ! AddPlayer(1, "", secondPlayerProbe.ref)
//        game ! StartGame
//
//        val settlement = Vertex(0)
//        val road = Edge(Vertex(0), Vertex(1))
//
//        game ! Response(1, InitialPlacementResponse(true, settlement, road))
//
//        assertMessageSentToProbe(firstPlayerProbe, InitialPlacementUpdate(1, true, settlement, road))
//        assertMessageSentToProbe(secondPlayerProbe, InitialPlacementUpdate(1, true, settlement, road))
//        assertMessageSentToProbe(firstPlayerProbe, InitialPlacementRequest(1, false))
//        assertMessageSentToProbe(secondPlayerProbe, InitialPlacementRequest(1, false))
//
//      }
//
//      it("from non-first player for second starting settlement should send an InitialPlacementUpdate " +
//        "message and InitialPlacementRequest message for second settlement for next player") {
//        val game = TestActorRef[GameActor](GameActor.props(singleHexBoard, stubbed6Dice, stubbedLog))
//        val firstPlayerProbe = TestProbe()
//        val secondPlayerProbe = TestProbe()
//
//        game ! AddPlayer(0, "", firstPlayerProbe.ref)
//        game ! AddPlayer(1, "", secondPlayerProbe.ref)
//        game ! StartGame
//
//        val settlement = Vertex(0)
//        val road = Edge(Vertex(0), Vertex(1))
//
//        game ! Response(1, InitialPlacementResponse(false, settlement, road))
//
//        assertMessageSentToProbe(firstPlayerProbe, InitialPlacementUpdate(1, false, settlement, road))
//        assertMessageSentToProbe(secondPlayerProbe, InitialPlacementUpdate(1, false, settlement, road))
//        assertMessageSentToProbe(firstPlayerProbe, InitialPlacementRequest(0, false))
//        assertMessageSentToProbe(secondPlayerProbe, InitialPlacementRequest(0, false))
//      }
//
//      it("from first player for second starting settlement should send an InitialPlacementUpdate " +
//        "message and TurnUpdate message for first player's turn") {
//        val game = TestActorRef[GameActor](GameActor.props(singleHexBoard, stubbed6Dice, stubbedLog))
//        val firstPlayerProbe = TestProbe()
//        val secondPlayerProbe = TestProbe()
//
//        game ! AddPlayer(0, "", firstPlayerProbe.ref)
//        game ! AddPlayer(1, "", secondPlayerProbe.ref)
//        game ! StartGame
//
//        val settlement = Vertex(0)
//        val road = Edge(Vertex(0), Vertex(1))
//
//        game ! Response(0, InitialPlacementResponse(false, settlement, road))
//
//        assertMessageSentToProbe(firstPlayerProbe, InitialPlacementUpdate(0, false, settlement, road))
//        assertMessageSentToProbe(secondPlayerProbe, InitialPlacementUpdate(0, false, settlement, road))
//        assertMessageSentToProbe(firstPlayerProbe, TurnUpdate(0))
//        assertMessageSentToProbe(secondPlayerProbe, TurnUpdate(0))
//      }
//    }
//
//    describe("RollDiceResponse") {
//
//    }
//
//    describe("MoveRobberResponse message") {
//
//      it("from current player should send a MoveRobberUpdate message to all players") {
//        val game = TestActorRef[GameActor](GameActor.props(CatanFixtures.baseBoard, stubbed6Dice, stubbedLog))
//        val firstPlayerProbe = TestProbe()
//        val secondPlayerProbe = TestProbe()
//
//        game ! AddPlayer(0, "", firstPlayerProbe.ref)
//        game ! AddPlayer(1, "", secondPlayerProbe.ref)
//        game ! StartGame
//      }
//
//    }
//
//
//  }
//
//}
