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

package stopwatch2

import stopwatch2.impl._

/**
 * Stopwatch group: used to initialize, start, dispose and reset stopwatches.
 * <p>
 * Stopwatches in the same group share the same percentile distribution.
 */
class StopwatchGroup(val name: String) {

  /** True if stopwatch factory is enabled (i.e., issues real stopwatches) */
  @volatile var enabled = false  // default to disabled

  /** Set to true if stopwatches are initialized implicitly when first used.
   *  Otherwise, non-preinitialized stopwatches are considered disabled. */
  @volatile var enableOnDemand = true  // default to enabled

  /** Time statistics. */
  // using j.u.c.ConcurrentHashMap because it performs better than any of the Scala collections
  // under concurrent workloads
  private val _stats = new java.util.concurrent.ConcurrentHashMap[String, StopwatchStatisticImpl]()

  private var _intervals: Array[Long] = null

  @volatile private var _listeners: List[StopwatchStatistic => Unit] = Nil

  /** Percentiles to track. */
  @volatile var percentiles: Array[Float] = Array(0.1f, 1.0f, 10.0f, 20.0f, 30.0f, 40.0f, 50.0f, 60.0f, 70.0f, 80.0f, 90.0f, 99.0f, 99.9f)

  /** Measure the elapsed time spent during execution of the provided function. */
  def apply[T](name: String)(f: => T): T = {
    val stopwatch = get(name)
    stopwatch.doWith(f)
  }

  /** Return a named stopwatch */
  private[stopwatch2] def get(name: String): Stopwatch = {
    if (!enabled) {
      return DisabledStopwatch
    }

    val existing = if (enableOnDemand) getOrCreate(name) else _stats.get(name)
    if (existing == null || !existing.enabled) {
      DisabledStopwatch
    } else {
      new EnabledStopwatch(name, existing)
    }
  }

  /** Return a named stopwatch and start gathering timing statistics for it. */
  def start(name: String): Stopwatch = {
    val stopwatch = get(name)
    stopwatch.start()
    stopwatch
  }

  /** Return a snapshot of the statistics for a given stopwatch. */
  def snapshot(name: String): StopwatchStatistic = {
    val stats = _stats.get(name)
    if (stats != null) return stats.snapshot
    else new StopwatchStatisticImpl(this, name)
  }

  /** Reset statistics for a given stopwatch. */
  def reset(name: String) = {
    val stats = _stats.get(name)
    if (stats != null) {
      stats.reset()
    }
  }

  /** Reset all stopwatches. */
  def resetAll() {
    for (s <- _stats.values) {
      s.reset()
    }
  }

  /** Enable a stopwatch. */
  def enable(name: String) = {
    val stats = getOrCreate(name)
    stats.enabled = true
    stats
  }

  /** Disable a stopwatch. */
  def disable(name: String): Unit = {
    val stats = _stats.get(name)
    if (stats != null) {
      stats.enabled = false
    }
  }

  /** Get the list of known stopwatches. */
  def names: Set[String] = _stats.keySet.toSet

  /** Dispose of a given stopwatch. */
  def dispose(name: String) = _stats.remove(name)

  /** Dispose of all stopwatches. */
  def disposeAll() = _stats.clear()

  /** Return existing statistics or create new statistics for given stopwatch */
  private def getOrCreate(name: String): StopwatchStatisticImpl = synchronized {
    val stats = _stats.get(name)
    if (stats != null) return stats
    val newStats = new StopwatchStatisticImpl(this, name)
    _stats.put(name, newStats)
    newStats
  }

  /** Add a stopwatch listener */
  def addListener(f: StopwatchStatistic => Unit): Unit = synchronized {
    _listeners = f :: _listeners
  }

  /** Remove a stopwatch listener */
  def removeListener(f: String => Unit): Unit = synchronized {
    _listeners = _listeners filter { l => !(l eq f) }
  }

  /** Notify all listeners that a Stopwatch has changed value */
  private[stopwatch2] def notifyListeners(stopwatch: StopwatchStatisticImpl) = {
    val list = _listeners
    if (!list.isEmpty) {
      val snapshot = stopwatch.snapshot()
      list.foreach { _(snapshot) }
    }
  }

  override def toString() = "StopwatchGroup('%s')".format(name)

  override def equals(other: Any) = other match {
    case other: StopwatchGroup => other.name == this.name
    case _ => false
  }

  override def hashCode = name.hashCode

  implicit def javaToScala[T](set: java.util.Set[T]): Set[T] = {
    var newSet = Set[T]()
    set.toArray.foreach { x => newSet += x.asInstanceOf[T] }
    newSet
  }

  implicit def javaToScala[T](set: java.util.Collection[T]): Iterable[T] = {
    var result = new scala.collection.mutable.ArrayBuffer[T]()
    set.toArray.foreach { x => result += x.asInstanceOf[T] }
    result
  }
}
