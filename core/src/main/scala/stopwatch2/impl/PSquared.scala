package stopwatch2.impl

import java.util.Arrays

/** The P-Square Algorithm for Dynamic Calculation of Percentiles and Histograms without Storing
 *  Observations by Jain and Chlamtac.
 *  http://www.cse.wustl.edu/~jain/papers/psqr.htm
 *
 *  Code imported from https://github.com/jacksonicson/psquared and mechanically converted from Java to Scala.
 *
 *  License is MIT.
 */
class PSquared(val p: Float) {

  if (p >= 1.0f) throw new IllegalArgumentException("p must be < 1.0f: " + p)
  if (p <= 0.0f) throw new IllegalArgumentException("p must be > 0.0f: " + p)

  private[this] val markers = 5

  var pValue: Float = _

  private[this] var initial: Array[Float] = new Array[Float](markers)

  private[this] var initialCount: Int = 0

  private[this] var initialized: Boolean = false

  private[this] var q: Array[Float] = new Array[Float](markers)

  private[this] var n: Array[Int] = new Array[Int](markers)

  private[this] var n_desired: Array[Float] = new Array[Float](markers)

  private[this] var dn: Array[Float] = new Array[Float](markers)

  private[this] var lastK: Int = _

  private def init() {
    initialized = true
    for (i <- 0 until markers) {
      q(i) = initial(i)
      n(i) = i
    }
    n_desired(0) = 0
    n_desired(1) = 2 * p
    n_desired(2) = 4 * p
    n_desired(3) = 2 + 2 * p
    n_desired(4) = 4
    dn(0) = 0
    dn(1) = p / 2f
    dn(2) = p
    dn(3) = (1f + p) / 2f
    dn(4) = 1
  }

  private def acceptInitial(x: Float): Boolean = {
    if (initialCount < markers) {
      initial(initialCount) = x
      initialCount += 1
      Arrays.sort(initial, 0, initialCount)
      return false
    }
    Arrays.sort(initial)
    init()
    true
  }

  private def initialSetPercentile(): Float = {
    val n = (p * initialCount.toFloat).toInt
    initial(n)
  }

  def accept(x: Float): Float = {
    if (!initialized) {
      if (!acceptInitial(x)) {
        pValue = initialSetPercentile()
        return pValue
      }
    }
    var k = -1
    if (x < q(0)) {
      q(0) = x
      k = 0
    } else if (q(0) <= x && x < q(1)) k = 0
    else if (q(1) <= x && x < q(2)) k = 1
    else if (q(2) <= x && x < q(3)) k = 2
    else if (q(3) <= x && x <= q(4)) k = 3
    else if (q(4) < x) {
      q(4) = x
      k = 3
    }
    assert((k >= 0))
    lastK = k
    for (i <- k + 1 until markers) n(i) += 1
    for (i <- 0 until markers) n_desired(i) += dn(i)
    for (i <- 1 until markers - 1) {
      val d = n_desired(i) - n(i)
      if ((d >= 1 && (n(i + 1) - n(i)) > 1) || (d <= -1 && (n(i - 1) - n(i)) < -1)) {
        val ds = sign(d)
        val tmp = parabolic(ds, i)
        q(i) = if (q(i - 1) < tmp && tmp < q(i + 1)) tmp else linear(ds, i)
        n(i) += ds
      }
    }
    pValue = q(2)
    q(2)
  }

  def linear(d: Int, i: Int): Float = q(i) + d * (q(i + d) - q(i)) / (n(i + d) - n(i))

  def parabolic(d: Float, i: Int): Float = {
    val a = d.toFloat / (n(i + 1) - n(i - 1)).toFloat
    val b = (n(i) - n(i - 1) + d).toFloat * (q(i + 1) - q(i)) / (n(i + 1) - n(i)).toFloat + (n(i + 1) - n(i) - d).toFloat * (q(i) - q(i - 1)) / (n(i) - n(i - 1)).toFloat
    q(i).toFloat + a * b
  }

  private def sign(d: Float): Int = if (d >= 0) 1 else -1

  override def toString = s"PSquared(p=$p, pValue=$pValue)"

  def toDebugString = {
    "PSquared(" +
      "p: " + p +
      ", pValue: " + pValue +
      ", initial: " + Arrays.toString(initial) +
      ", initialCount: " + initialCount +
      ", markers: " + markers +
      ", k: " + lastK +
      ", q: " + Arrays.toString(q) +
      ", n: " + Arrays.toString(n) +
      ", n': " + Arrays.toString(n_desired) +
      ")"
  }
}