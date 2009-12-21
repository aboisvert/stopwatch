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
 * Time statistics collected by a stopwatch.
 * <p>
 * Wall clock values are provided in milliseconds (from System.currentTimeMillis).
 * Other measured time values are in nanoseconds (from System.nanoTime).
 */
trait StopwatchStatistic {

  def name: String

  def enabled: Boolean

  /** Time when stopwatch was first accessed */
  def firstAccessTime: Option[Long]

  /** Time when stopwatch was last accessed */
  def lastAccessTime: Option[Long]

  /** Number of times the stopwatch was started */
  def hits: Long

  /** Returns the average amount of time taken to complete one invocation of
   *  this operation since the beginning of this measurement (in nanoseconds).
   */
  def averageTime: Long

  /** Returns the total amount of time spent by threads inside the stopwatch (in nanoseconds). */
  def totalTime: Long

  /** Returns the minimum amount of time spent by any given thread inside the stopwatch (in nanoseconds). */
  def minTime: Long

  /** Returns the maximum amount of time spent by any given thread inside the stopwatch (in nanoseconds). */
  def maxTime: Long

  /** Returns the range used for recording execution time distribution */
  def range: StopwatchRange

  /** Returns execution time distribution over the defined range. */
  def distribution: Seq[Long]

  /** Returns the number of hits with execution time under range. */
  def hitsUnderRange: Long

  /** Returns the number of hits with execution time over range. */
  def hitsOverRange: Long

  /** Returns the standard deviation of time for each event (in nanoseconds). */
  def standardDeviationTime: Long

  /** Returns current number of threads inside the stopwatch section */
  def currentThreads: Long

  /** Returns maximum number of threads inside the stopwatch section */
  def maxThreads: Long

  /** Returns average number of threads inside the stopwatch section */
  def averageThreads: Float

  /** Returns a short string representation of stopwatch statistics */
  def toShortString: String

  /** Returns a medium-length string representation of stopwatch statistics */
  def toMediumString: String

  /** Returns a long string representation of stopwatch statistics,
   *  including time distribution.
   */
  def toLongString: String
}
