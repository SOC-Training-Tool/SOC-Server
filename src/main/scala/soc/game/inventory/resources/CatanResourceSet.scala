package soc.game.inventory.resources

import soc.game.inventory._

case class CatanResourceSet[T: Numeric](br: T = 0, or: T = 0, sh: T = 0, wh: T = 0, wo: T = 0) extends CatanSet[Resource, T] {

  override protected val implWrap: NumericWrapper = NumericWrapper()

  override def _copy(map: Map[Resource, T]): CatanResourceSet[T] = CatanResourceSet(
    map.get(Brick).getOrElse(implWrap.wrapped.zero),
    map.get(Ore).getOrElse(implWrap.wrapped.zero),
    map.get(Sheep).getOrElse(implWrap.wrapped.zero),
    map.get(Wheat).getOrElse(implWrap.wrapped.zero),
    map.get(Wood).getOrElse(implWrap.wrapped.zero)
  )

  override val amountMap: Map[Resource, T] = Map(Brick -> br, Ore -> or, Sheep -> sh, Wheat -> wh, Wood -> wo)

}

object CatanResourceSet {
  type CatanResourceSet[S] = CatanSet[Resource, S]
  type ResourceSet[S] = CatanResourceSet[S]
  type Resources = ResourceSet[Int]

  def empty[T: Numeric]: CatanResourceSet[T] = {
    val num = implicitly[Numeric[T]]
    CatanResourceSet(num.zero, num.zero, num.zero, num.zero, num.zero)

  }
  val fullBank = CatanResourceSet(19, 19, 19, 19, 19)


  def apply[T](resMap: Map[Resource, T])(implicit num: Numeric[T]): ResourceSet[T] =
    CatanResourceSet[T](
      resMap.get(Brick).getOrElse(num.zero),
      resMap.get(Ore).getOrElse(num.zero),
      resMap.get(Sheep).getOrElse(num.zero),
      resMap.get(Wheat).getOrElse(num.zero),
      resMap.get(Wood).getOrElse(num.zero))

  def apply(resources: Resource*): Resources = {
    resources.foldLeft(empty[Int]) { case (set, res) => set.add(CatanResourceSet(Map(res -> 1)))}
  }


  def describe(set: Resources): String = {
    set.amountMap.filter(_._2 > 0).map { case (res, amt) => s"$amt ${res.name}s"}.mkString(", ")
  }
}

