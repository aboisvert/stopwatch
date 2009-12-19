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
 * All time values are provided in milliseconds.
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
   *  this operation since the beginning of this measurement.
   */
  def averageTime: Float

  /** Returns the total amount of time spent by threads inside the stopwatch. */
  def totalTime: Long

  /** Returns the minimum amount of time spent by any given thread inside the stopwatch. */
  def minTime: Long

  /** Returns the maximum amount of time spent by any given thread inside the stopwatch. */
  def maxTime: Long

  /** Returns the range used for hit distribution */
  def range: StopwatchRange

  /** Returns hit distribution per interval. */
  def distribution: Seq[Long]

  /** Returns the number of hits under range. */
  def hitsUnderRange: Long
  
  /** Returns the number of hits over range. */
  def hitsOverRange: Long

  /** Returns the standard deviation of time for each event. */
  def standardDeviationTime: Long
  
  /** Returns current number of threads inside the stopwatch section */
  def currentThreads: Long

  /** Returns maximum number of threads inside the stopwatch section */
  def maxThreads: Long

  /** Returns average number of threads inside the stopwatch section */
  def averageThreads: Float
  
}
