package soc.game

import org.scalatest.{FunSpec, Matchers}

class CombinationMapIteratorSpec extends FunSpec with Matchers {

  describe("getIterator") {




    it("") {
      val map = Map (
        0 -> (1 to 3).toList.permutations,
        1 -> (4 to 6).toList.permutations,
        2 -> (7 to 9).toList.permutations
      )
      val iter = CombinationMapIterator.getIterator(map)
      iter.foreach(println)
    }

    it("a") {
      val iter = (1 to 3).toList.permutations
      val (iterA, iterB) = iter.duplicate
      val combIter = CombinationMapIterator.getIterator(Map(0 -> iterA))
      combIter.length shouldEqual iterB.size


    }



  }

}
