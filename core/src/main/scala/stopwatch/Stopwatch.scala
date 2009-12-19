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
 * Default Stopwatch group to create stopwatches.
 */
object Stopwatch extends StopwatchGroup("Stopwatch")

/**
 * The stopwatch interface used to measure elapsed time and other performance metrics.
 */
trait Stopwatch {

  /** Return the name of this stopwatch. */
  def name: String

  /** Returns true if stopwatch is enabled */
  def enabled: Boolean

  /** Start the stopwatch.  The stopwatch measures time until stop() is called. */
  def start(): Unit

  /** Stop measuring time.
   *
   * @exception IllegalStateException if stopwatch wasn't already started.
   */
  def stop(): Unit

  /** Measure the elapsed time spent during execution of the provided function.
   * <p>
   * This is the equivalent of <code> start(); try { f() } finally { stop() }  <code>
   */
  def doWith[T](f: => T): T = {
    start()
    try {
      f
    } finally {
      stop()
    }
  }

}
