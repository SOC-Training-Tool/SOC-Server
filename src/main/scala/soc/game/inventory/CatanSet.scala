package soc.game.inventory

trait CatanSet[A <: InventoryItem, T] {

  protected case class NumericWrapper(implicit val wrapped: Numeric[T])
  protected val implWrap: NumericWrapper
  import implWrap.wrapped

  protected def _copy(map: Map[A, T]): CatanSet[A, T]

  val amountMap: Map[A, T]

  lazy val getTotal = amountMap.values.sum
  lazy val getTypes: Seq[A] = amountMap.keys.toSeq
  lazy val getTypeCount = amountMap.values.count(wrapped.toDouble(_) > 0)
  lazy val isEmpty: Boolean = getTotal == 0

  def add(amt: T, a: A): CatanSet[A, T] = {
    val curAmount = amountMap.get(a)
    _copy(curAmount.fold(amountMap + (a -> amt)){ amount =>
      (amountMap - a) + (a -> wrapped.plus(amount, amt))
    })
  }

  def add(set: CatanSet[A, T]): CatanSet[A, T] = {
    set.getTypes.foldLeft(this){ case (newSet: CatanSet[A, T], a: A) => newSet.add(set(a), a) }
  }

  def subtract(amt: T, a: A): CatanSet[A, T] = {
    val curAmount = amountMap.get(a)
    val newMap = curAmount.fold(amountMap) { amount =>
      (amountMap - a) + (a -> wrapped.max(wrapped.zero, wrapped.minus(amount, amt)))
    }
    _copy(newMap)
  }

  def subtract(set: CatanSet[A, T]): CatanSet[A, T] = {
    set.getTypes.foldLeft(this){ case (newSet: CatanSet[A, T], a: A) => newSet.subtract(set(a), a) }
  }

  def contains(amt: T, a: A): Boolean = amountMap.get(a).fold(false)(wrapped.gteq(_, amt))
  def contains(a: A): Boolean = amountMap.get(a).fold(false)(wrapped.gt(_, wrapped.zero))
  def contains(set: CatanSet[A, T]): Boolean = {
    set.getTypes.filter(set.contains).forall(res => contains(set.getAmount(res), res))
  }

  def getAmount(a: A): T = amountMap.getOrElse(a, wrapped.zero)

  def apply(a: A): T = getAmount(a)
}

object CatanSet {

  def toList[A <: InventoryItem](set: CatanSet[A, Int]): List[A] = set.amountMap.flatMap { case (a, amt) => (1 to amt).map(_ => a)}.toList

}