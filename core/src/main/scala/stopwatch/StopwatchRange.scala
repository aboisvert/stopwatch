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

package stopwatch

/**
 * Range of stopwatch hit distribution.
 */
case class StopwatchRange(lowerBound: TimeUnit, higherBound: TimeUnit, step: TimeUnit) {
  private val _low = lowerBound.toNanos
  private val _high = higherBound.toNanos
  private val _step = step.toNanos

  /** Returns number of range intervals */
  def intervals: Int = (_high-_low) / _step toInt

  /** Returns which interval a value (in nanos) falls into.  Does not perform range checking. */
  def interval(x: Long): Int = {
    if (x < _low) (x-_low-_step) / _step toInt
    else (x-_low) / _step toInt
  }

  /** Returns which interval a value falls into.  Does not perform range checking. */
  def interval(x: TimeUnit): Int = interval(x.toNanos)

  /** Returns a sequence of tuples (lowerBound, upperBounds) for each interval (in nanos).
   *  e.g.,
   *       (  0,   999)
   *       (1000, 1999)
   *       (2000, 2999)
   *       (3000, 3999)
   *       (4000, 4999)
   */
  def intervalsAsTuple: Seq[(Long, Long)] = {
    var i = 0
    val max = intervals
    var l = new Array[(Long, Long)](max)
    while (i < max) {
      val lower = _low + (i*_step)
      l(i) = (lower, lower + _step)
      i += 1
    }
    l.toSeq
  }

  /** Spread = higherBound-lowerBound (in nanos) */
  def spread: Long = _high-_low

  def toList: List[Long] = {
    val max = intervals
    var l = List[Long]()
    var n = 0L
    while (n < max) {
      l ::= n
      n += _step
    }
    l.reverse
  }
}