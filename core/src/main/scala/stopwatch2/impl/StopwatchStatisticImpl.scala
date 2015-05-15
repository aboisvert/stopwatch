/*
 *  Copyright 2009-2010 Alex Boisvert
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package stopwatch2.impl

import stopwatch2._
import scala.concurrent.duration._

/**
 * Time statistics for a stopwatch.
 */
final class StopwatchStatisticImpl(val group: StopwatchGroup, val name: String)
  extends StopwatchStatistic
  with Cloneable
{

  /** Whether the stopwatch is enabled */
  @volatile var enabled: Boolean = true // enabled by default

  /** Number of start "hits" */
  private var _hits: Long = 0

  /** Number of errors */
  private var _errors: Long = 0

  /** Total time, represents the elapsed time while the stopwatch was started. */
  private var _totalTime: FiniteDuration = Duration.Zero

  /** Minimum time spent in the stopwatch. */
  private var _minTime: Duration = Duration.Undefined

  /** Maximum time spent in the stopwatch. */
  private var _maxTime: Duration = Duration.Undefined

  /** Average time spent in the stopwatch. */
  private var _averageTime: Duration = Duration.Undefined

  /** Standard deviation of time spent in the stopwatch. */
  private var _standardDeviationTime: Duration = Duration.Zero

  /** Maximum number of threads operating on this stopwatch at once. */
  private var _maxThreads: Int = 0

  /** Number of threads currently operating on this stopwatch. */
  private var _currentThreads: Int = 0

  /** Average number of threads when stopwatch is entered. */
  private var _averageThreads: Float = 0

  /** Time when stopwatch was first accessed. */
  private var _firstAccessTime: Option[Long] = None

  /** Time when stopwatch was last accessed. */
  private var _lastAccessTime: Option[Long] = None

  /** Used to calculate the average number of threads */
  private var _totalThreadsDuringStart: Long = 0

  /** Used to calculate the standard deviation. */
  private var _sumOfSquares: Double = 0.0d

  /** PSquared instances for each percentile */
  private var _psquared: Array[PSquared] = initPSquared()

  private def initPSquared(): Array[PSquared] = {
    if (group.percentiles exists { p => p >= 100.0f || p <= 0.0f }) {
      throw new IllegalArgumentException("Percentiles must be 0% < p < 100%: " + group.percentiles)
    }
    val array = new Array[PSquared](group.percentiles.length)
    var i = 0
    while (i < array.length) {
      array(i) = new PSquared(group.percentiles(i) / 100.0f)
      i += 1
    }
    array
  }

  def hits = _hits

  def errors = _errors

  def totalTime = _totalTime

  def minTime = _minTime

  def maxTime = _maxTime

  def averageTime = _averageTime

  def standardDeviationTime = _standardDeviationTime

  def maxThreads = _maxThreads

  def currentThreads = _currentThreads

  def averageThreads = _averageThreads

  def firstAccessTime = _firstAccessTime

  def lastAccessTime = _lastAccessTime

  def percentiles: IndexedSeq[Percentile] = {
    (group.percentiles.iterator zip _psquared.iterator)
      .map { case (p, psquared) => Percentile(p, psquared.pValue.nanos) }
      .to[IndexedSeq]
  }

  /**
   * Update statistics following stopwatch start event.
   */
  private[impl] def notifyStart(currentTime: Long) = synchronized {
    _currentThreads += 1
    _hits += 1
    _totalThreadsDuringStart += _currentThreads
    _lastAccessTime = Some(currentTime)
    if (_currentThreads > _maxThreads) _maxThreads = _currentThreads
    if (_hits == 1) _firstAccessTime = Some(currentTime)
  }

  /**
   * Update statistics following stopwatch stop event.
   */
  private[impl] def notifyStop(currentTime: Long, elapsed: FiniteDuration, error: Boolean) = {
    synchronized {
      _currentThreads -= 1
      _totalTime += elapsed
      _lastAccessTime = Some(currentTime)
      _sumOfSquares += elapsed.toNanos * elapsed.toNanos

      if (error) _errors += 1

      if (!_maxTime.isFinite || _maxTime < elapsed) _maxTime = elapsed
      if (!_minTime.isFinite || _minTime > elapsed) _minTime = elapsed

      var i = 0
      while (i < _psquared.length) {
        _psquared(i).accept(elapsed.toNanos)
        i += 1
      }
    }
    group.notifyListeners(this)
  }

  // override StopwatchStatistic.reset()
  def reset() = synchronized {
    _hits = 0
    _errors = 0
    _totalTime = Duration.Zero
    _minTime = Duration.Undefined
    _maxTime = Duration.Undefined
    _firstAccessTime = None
    _lastAccessTime  = None
    _maxThreads = 0
    _totalThreadsDuringStart = 0
    _sumOfSquares = 0
    _psquared = initPSquared()
  }

  /** Take a snapshot of the StopwatchStatistic. */
  def snapshot() = {
    // clone() is synchronized -- see below
    val snapshot = clone().asInstanceOf[StopwatchStatisticImpl]

    // calculate average threads & average time
    if (snapshot._hits == 0) {
      snapshot._averageThreads = 0.0f
      snapshot._averageTime = Duration.Undefined
    } else {
      snapshot._averageThreads = snapshot._totalThreadsDuringStart / snapshot._hits
      snapshot._averageTime = snapshot._totalTime / snapshot._hits
    }

    /* Calculate standard deviation:
            hits - count of all x's
            numerator = sum(xi^2) - sum(xi)^2/n
            std_dev = square_root(numerator / (n-1))
     */
    snapshot._standardDeviationTime = Duration.Undefined
    if (snapshot._hits != 0) {
      val sumOfX: Double = snapshot._totalTime.toNanos
      val n = snapshot._hits
      val nMinus1: Double = (n-1) max 1
      val numerator: Double = snapshot._sumOfSquares - ((sumOfX * sumOfX) / n)
      snapshot._standardDeviationTime = java.lang.Math.sqrt(numerator.toDouble / nMinus1).toLong.nanos
    }
    snapshot
  }

  override def clone() = synchronized {
    val cloned = super.clone().asInstanceOf[StopwatchStatisticImpl]

    // the default clone of an array is shallow.
    if (_psquared != null) {
      cloned._psquared = _psquared.toArray // make a copy
    }
    cloned
  }

  override def hashCode = name.hashCode

  override def equals(other: Any): Boolean = other match {
    case x: StopwatchStatistic => name == x.name
    case _ => false
  }

  override def toString() = toMediumString

  private def toMillis(d: Duration) = {
    if (d.isFinite) d.toMillis + "ms"
    else "N/A"
  }

  /** Returns a short string representation of stopwatch statistics */
  def toShortString = {
    "Stopwatch \"%s\" {hits=%d, errors=%d, min=%s, avg=%s, max=%s, total=%s, stdDev=%s}".
      format(name, _hits, _errors, toMillis(_minTime), toMillis(_averageTime), toMillis(_maxTime),
             toMillis(_totalTime), toMillis(_standardDeviationTime))
  }

  /** Returns a medium-length string representation of stopwatch statistics */
  def toMediumString = {
    val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z")
    val delta = for {
      last <- _lastAccessTime
      first <-_firstAccessTime
    } yield last-first

    val throughput = for (d <- delta) yield (_hits * 1000 / (d: Float))

    def formatTime(time: Option[Long]) = time.map(dateFormat.format(_)) getOrElse "N/A"

    ("Stopwatch \"%s\" {hits=%d, throughput=%.3f/s, errors=%d, " +
     "minTime=%s, avgTime=%s, maxTime=%s, totalTime=%s, stdDev=%s, " +
     "currentThreads=%d, avgThreads=%.2f, maxThreads=%d, " +
     "first=%s, last=%s}" ).format(
      name, _hits, throughput getOrElse -1.0, _errors,
      toMillis(_minTime), toMillis(_averageTime), toMillis(_maxTime), toMillis(_totalTime), toMillis(_standardDeviationTime),
      _currentThreads, _averageThreads, _maxThreads,
      formatTime(firstAccessTime), formatTime(_lastAccessTime))
  }

  /** Returns a long string representation of stopwatch statistics,
   *  including time distribution.
   */
  def toLongString = {
    toMediumString +
      " Percentiles {" +
      (percentiles map { p => "(%.2f%%, %s)" format (p.p, toMillis(p.pValue)) }).mkString(", ") +
      "}"
  }
}
