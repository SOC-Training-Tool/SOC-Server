package soc.player
import soc.game.player.{NoInfoPlayerState, PlayerState}

class NoInfoPlayerStateSpec extends PlayerStateSpec {
  override def getPlayer(name: String, position: Int): PlayerState = NoInfoPlayerState(name, position)
}
