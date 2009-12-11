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
case class StopwatchRange(lowerBound: Long, higherBound: Long, step: Long) {
  /** Returns number of range intervals */
  def intervals: Int = (higherBound-lowerBound) / step toInt
  
  /** Returns which interval a value falls into.  (Does not perform range checking) */
  def interval(x: Long): Int = {
    if (x < lowerBound) (x-lowerBound-step) / step toInt
    else (x-lowerBound) / step toInt
  }
  
  /** Returns a sequence of tuples (lowerBound, upperBounds) for each interval.
   *  e.g.,
   *       (  "0",   "999")
   *       ("1000", "1999")
   *       ("2000", "2999")
   *       ("3000", "3999")
   *       ("4000", "4999")
   */
  def intervalsAsTuple: Seq[(Long, Long)] = {
    var i = 0
    var max = intervals
    var l = new Array[(Long, Long)](intervals)
    while (i < intervals) {
      val lower = lowerBound + (i*step) 
      l(i) = (lower, lower + step)
      i += 1
    }
    l.toSeq
  }

}