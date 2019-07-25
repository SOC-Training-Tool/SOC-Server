package soc.controller

class GameController {

  def addGame: Unit = ()

}

object GameController {

  sealed trait GameStatus
  case object Waiting extends GameStatus
  case object Playing extends GameStatus
  case object Done extends GameStatus

}
