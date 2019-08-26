//package soc.game.inventory.resources
//
//import org.scalatest.{FunSpec, Matchers}
//import soc.game._
//import soc.game.inventory._
//import soc.game.inventory.resources.{CatanResourceSet, Gain, Lose, SOCPossibleHands, Steal}
//
//class SOCPossibleHandsSpec extends FunSpec with Matchers {
//
//  val possHands = SOCPossibleHands.empty
//  val resourceSet = CatanResourceSet[Int](3, 2, 0, 0, 0)
//
//  describe("handsForPlayers") {
//
//    it ("should return empty map if hands are all empty") {
//      possHands.handsForPlayers shouldBe empty
//    }
//
//    it("should return a nonempty map if a resource set is gained") {
//      val gain = possHands.calculateHands(List(Gain(0, resourceSet)))
//      gain.handsForPlayers should not be empty
//    }
//
//    it("") {
//      val steal = possHands.calculateHands(List(Gain(0, resourceSet), Steal(1, 0, None), Steal(1, 0, None)))
//
//      println(steal)
//      println(steal.handsForPlayers)
//
//      steal.handsForPlayers(1) should contain (CatanResourceSet(2, 0, 0, 0, 0))
//    }
//
//    it("a") {
//      val steal1 = possHands.calculateHands(List(Gain(0, resourceSet), Steal(1, 0, None)))
//      val steal2 = possHands.calculateHands(List(Gain(0, resourceSet), Steal(1, 0, None), Steal(1, 0, None)))
//      println(s"A: hands${steal1.handsForPlayers} prob${steal1.probableHands}")
//      println(s"B: hands${steal2.handsForPlayers} prob${steal2.probableHands}")
//
//    }
//
//    it("All hands for player should be the same if player recieved perfect info") {
//      val transactions = List(
//        Gain(0, CatanResourceSet(Wood, Brick)),
//        Gain(0, CatanResourceSet(Wheat, Sheep)),
//        Gain(0, CatanResourceSet(Ore, Ore, Ore, Wheat, Wheat)),
//        Lose(0, CatanResourceSet(Wood, Brick)),
//        Gain(1, CatanResourceSet(Wood, Brick)),
//        Steal(0, 1, Some(CatanResourceSet(Wood))),
//        Steal(0, 1, Some(CatanResourceSet(Brick))),
//        Steal(1, 0, Some(CatanResourceSet(Ore)))
//      )
//
//      val possibleHands = possHands.calculateHands(transactions)
//      println(possibleHands.handsForPlayers(0).distinct)
//
//
//
//    }
//
//  }
//
//  describe("toProbableHands") {
//
//    it("should return empty map if hands are all empty") {
//      possHands.probableHands shouldBe empty
//    }
//
//    it("should return a nonempty map if a resource set is gained") {
//      val gain = possHands.calculateHands(List(Gain(0, resourceSet)))
//      gain.probableHands should not be empty
//    }
//
//    it("a") {
//      val steal = possHands.calculateHands(List(Gain(0, resourceSet), Steal(1, 0, None), Steal(1, 0, None)))
//
//      println(steal)
//      println(steal.probableHands)
//
//    }
//
//
//
//  }
//
//  describe ("playerGainCards") {
//
//    describe("should add player and player's hand") {
//
//      it("when all hands are empty") {
//        val handsWithPlayer = possHands.playerGainCards(0, resourceSet)
//        handsWithPlayer.hands should have length 1
//        handsWithPlayer.hands.head should contain only (0 -> resourceSet)
//      }
//
//
//
//    }
//
//  }
//
//  describe("playerLoseCards") {
//
//  }
//
//  describe("stealUnknownCards") {
//
//  }
//
//
//
//
//
//}
