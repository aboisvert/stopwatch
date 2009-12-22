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
import stopwatch.StopwatchGroup
import stopwatch.StopwatchStatistic

/**
 * Singleton disabled stopwatch
 */
object DisabledStopwatch extends DisabledStopwatch

/**
 * Permanently disabled stopwatch, which does not measure anything.
 */
class DisabledStopwatch
  extends Stopwatch
{
  val name = "DISABLED"
  val enabled = false
  def reset() = ()
  def start() = ()
  def stop() = ()
  def error() = ()
  def snapshot() = Statistic
  override def doWith[T](f: => T): T = f
  
  val Statistic = new StopwatchStatisticImpl(new StopwatchGroup("DISABLED"), "DISABLED")
  
}
