package soc.game.dice

import scala.util.Random

case class NormalDice(implicit rand: Random) extends Dice {

  val sides = 6

  def randomRoll = rand.nextInt(sides) + 1

  def getRoll: (Int, Int) = (randomRoll, randomRoll)



}
