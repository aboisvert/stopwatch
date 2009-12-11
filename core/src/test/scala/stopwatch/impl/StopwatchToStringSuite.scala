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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import stopwatch.StopwatchFactory
import stopwatch.StopwatchRange

object StopwatchToStringSuiteRunner {
  def main(args: Array[String]) = (new StopwatchToStringSuite).execute
}

class StopwatchToStringSuite extends FunSuite with ShouldMatchers {
  
  test("Stopwatch.toString and variants") {
    val factory = new StopwatchFactory
    factory.enabled = true
    factory.range = StopwatchRange(0, 300, 100)
    
    val now = System.currentTimeMillis()
    var stats = factory.snapshot("foo").asInstanceOf[stopwatch.impl.StopwatchStatisticImpl]
    stats.notifyStart(now+0)
    stats.notifyStop(now+50, 50)

    stats.notifyStart(now+0)
    stats.notifyStop(now+99, 99)

    stats.notifyStart(now+0)
    stats.notifyStop(now+100, 100)

    stats.notifyStart(now+0)
    stats.notifyStop(now+199, 199)

    stats.notifyStart(now+0)
    stats.notifyStop(now+200, 200)

    stats.notifyStart(now+200)
    stats.notifyStop(now+700, 500)

    stats = stats.snapshot // force calc of stats
    
    Console println stats.toShortString
    Console println stats.toMediumString
    Console println stats.toLongString
  }
}