package log

class SystemLog extends Log {

  override def print(message: String): Unit = println(message)
}
