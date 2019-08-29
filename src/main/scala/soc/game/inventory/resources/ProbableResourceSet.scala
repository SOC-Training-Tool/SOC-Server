package soc.game.inventory.resources

import CatanResourceSet.{ResourceSet, Resources}
import soc.game.inventory.Resource

class ProbableResourceSet(known: ResourceSet[Int], unknown: ResourceSet[Double]) {

  def getTotalProbableAmount(resourceType: Resource): Double = getAmount(resourceType) + getProbableAmount(resourceType)

  /**
    * How many resources of this type are contained in the set?
    *
    * @param resourceType the type of resource, like { @link SOCResourceConstants#CLAY}
    * @return the number of a kind of resource
    * @see #contains(int)
    * @see #getTotal()
    */
  def getAmount(resourceType: Resource): Int = known.getAmount(resourceType)

  def getProbableAmount(resourceType: Resource): Double = unknown.getAmount(resourceType)


  /**
    * Does the set contain any resources of this type?
    *
    * @param resourceType the type of resource, like { @link SOCResourceConstants#CLAY}
    * @return true if the set's amount of this resource &gt; 0
    * @see #getAmount(int)
    * @see #contains(ResourceSet)
    */
  def contains(resourceType: Resource): Boolean = getAmount(resourceType) > 0

  def mightContain(resourceType: Resource): Boolean = getTotalProbableAmount(resourceType) > 0

  def probabilityContains(resourceType: Resource): Double = {
    if (contains(resourceType)) 1.0
    else getProbableAmount(resourceType) / getTotal
  }

  def getProbabilityOfResourceInHand(resourceType: Resource): Double = getTotalProbableAmount(resourceType) / getTotal

  /**
    * Get the number of known resource types contained in this set:
    * {@link SOCResourceConstants#CLAY} to {@link SOCResourceConstants#WOOD},
    * excluding {@link SOCResourceConstants#UNKNOWN} or {@link SOCResourceConstants#GOLD_LOCAL}.
    * An empty set returns 0, a set containing only wheat returns 1,
    * that same set after adding wood and sheep returns 3, etc.
    *
    * @return The number of resource types in this set with nonzero resource counts.
    */
  val getResourceTypeCount: Int = known.getTypeCount

  val getResourceTypeMightCount: Int = Resource.list.filter(mightContain).length

  /**
    * Get the total number of resources in this set
    *
    * @return the total number of resources
    * @see #getAmount(int)
    */
  val getTotal: Int = getKnownTotal + getUnknownTotal

  val getKnownTotal: Int = known.getTotal

  val getUnknownTotal: Int =  unknown.getTotal.toInt

  /**
    * @return true if this contains at least the resources in other
    * @param other the sub set, can be { @code null} for an empty resource subset
    * @see #contains(int)
    */
  def contains(other: Resources): Boolean = known.contains(other)

  def mightContain(other: Resources): Boolean =  Resource.list.forall { res => getTotalProbableAmount(res).ceil >= other.getAmount(res) }

  lazy val toUnknown: ProbableResourceSet = ProbableResourceSet.unknown(
    known.amountMap.foldLeft(unknown){ case (unknown, (res, amt)) => unknown.add(amt.toDouble, res)}
  )


  override val toString: String =  Resource.list.filter(getTotalProbableAmount(_) > 0).map { res: Resource =>
    s"${res.name}= ${known.getAmount(res)}:${unknown.getAmount(res)}"
  }.mkString(", ")

  val knownWithProbabilityUnknown: String =  Resource.list.filter(getTotalProbableAmount(_) > 0).map { res: Resource =>
    s"${res.name}= ${getAmount(res) + (if(getUnknownTotal > 0) getProbableAmount(res) / getUnknownTotal else 0)}"
  }.mkString(", ")

  def copy(known: ResourceSet[Int] = known, unknown: ResourceSet[Double] = unknown): ProbableResourceSet = {
    new ProbableResourceSet(known.asInstanceOf[CatanResourceSet[Int]].copy(), unknown.asInstanceOf[CatanResourceSet[Double]].copy())
  }
}

object ProbableResourceSet {

  def empty = new ProbableResourceSet(CatanResourceSet.empty[Int], CatanResourceSet.empty[Double])

  def known(known: ResourceSet[Int]): ProbableResourceSet = new ProbableResourceSet(known, CatanResourceSet.empty[Double])
  def unknown(unknown: ResourceSet[Double]): ProbableResourceSet = new ProbableResourceSet(CatanResourceSet.empty[Int], unknown)
}


