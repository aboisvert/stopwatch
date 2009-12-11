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

package stopwatch.impl

import stopwatch.Stopwatch

/**
 * Enabled stopwatch that collects statistics.
 */
final class EnabledStopwatch(val name: String, private var _stats: StopwatchStatisticImpl) extends Stopwatch {
  /**
   * Time when stopwatch was last started, or 0 if not started
   */
  private var _start: Long = 0

  // implement Stopwatch
  def start() {
    if (_start != 0) {
      throw new IllegalStateException("Stopwatch already started:" + _stats.name)
    }
    val currentTime = System.currentTimeMillis
    _start = currentTime
    _stats.notifyStart(currentTime)
  }

  def stop() {
    if (_start == 0) {
      throw new IllegalStateException("Stopwatch not started: " + _stats.name)
    }
    val currentTime = System.currentTimeMillis
    val elapsed = currentTime - _start
    _stats.notifyStop(currentTime, elapsed)
    _start = 0
  }

  override def hashCode = name.hashCode 

  override def equals(other: Any): Boolean = other match {
    case x: Stopwatch => name == x.name
    case _ => false
  }

  override def toString() = _stats.toString
}
