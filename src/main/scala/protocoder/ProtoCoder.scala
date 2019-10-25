package protocoder

trait ProtoCoder[A, B] {
  def proto(a: A): B
}

object ProtoCoder {

  def apply[A, B](implicit encoder: ProtoCoder[A, B]): ProtoCoder[A, B] = encoder

  object ops {
    def proto[A, B](a: A)(implicit encoder: ProtoCoder[A, B]): B = ProtoCoder[A, B].proto(a)

    implicit class ProtoCoderOps[A, B](a: A)(implicit encoder: ProtoCoder[A, B]) {
      def proto: B = ProtoCoder[A, B].proto(a)
    }
  }

}
