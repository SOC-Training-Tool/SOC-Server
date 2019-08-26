package soc.game


object CombinationMapIterator {

  def getIterator[K, V](iteratorMap: Map[K, Iterator[V]]): Iterator[Map[K, V]] = {
    iteratorMap.foldLeft(empty[K, V]){ case (innerIter, (k, iter)) => new CombinationMapIteratorImpl(k, iter, innerIter) }
  }

  def empty[K, V]: Iterator[Map[K, V]] = new EmptyCombinationMapIterator[K, V]

}

private trait CombinationMapIterator[K, V] extends Iterator[Map[K, V]]

private class CombinationMapIteratorImpl[K, V](k: K, iter: Iterator[V], var innerIterParam: Iterator[Map[K, V]]) extends CombinationMapIterator[K, V] {
  var (innerIter, innerIterDuplicate) = innerIterParam.duplicate
  var n: V = (if (innerIter.hasNext) iter.next() else null).asInstanceOf[V]

  override def hasNext: Boolean = iter.hasNext || innerIter.hasNext

  override def next(): Map[K, V] = {
    if (!innerIter.hasNext) {
      n = iter.next
      val (a, b) = innerIterDuplicate.duplicate
      innerIter = a
      innerIterDuplicate = b
    }
    innerIter.next + (k -> n)
  }
}

private class EmptyCombinationMapIterator[K, V] extends CombinationMapIterator[K, V] {
  override def hasNext: Boolean = false

  override def next(): Map[K, V] = Map.empty
}


