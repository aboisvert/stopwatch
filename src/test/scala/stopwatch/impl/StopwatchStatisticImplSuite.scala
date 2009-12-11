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

object StopwatchStatisticImplSuiteRunner extends StopwatchStatisticImplSuite {
  def main(args: Array[String]) = (new StopwatchStatisticImplSuite).execute
}

class StopwatchStatisticImplSuite extends FunSuite with ShouldMatchers {

  test("Distribution intervals") {
    val factory = new StopwatchFactory
    factory.enabled = true
    factory.range = StopwatchRange(0, 400, 100)
    
    val s1 = factory.snapshot("foo").asInstanceOf[stopwatch.impl.StopwatchStatisticImpl]

    s1.notifyStart(0)
    s1.notifyStop(50, 50)
    
    var stats = s1
    stats.distribution.length should be === 4
    stats.distribution(0) should be === 1L
    stats.distribution(1) should be === 0L
    stats.distribution(2) should be === 0L
    stats.distribution(3) should be === 0L

    s1.notifyStart(0)
    s1.notifyStop(99, 99)
    stats.distribution(0) should be === 2L
    stats.distribution(1) should be === 0L
    stats.distribution(2) should be === 0L
    stats.distribution(3) should be === 0L
    
    s1.notifyStart(0)
    s1.notifyStop(100, 100)
    stats.distribution(0) should be === 2L
    stats.distribution(1) should be === 1L
    stats.distribution(2) should be === 0L
    stats.distribution(3) should be === 0L
    
    s1.notifyStart(0)
    s1.notifyStop(199, 199)
    stats.distribution(0) should be === 2L
    stats.distribution(1) should be === 2L
    stats.distribution(2) should be === 0L
    stats.distribution(3) should be === 0L
    
    s1.notifyStart(0)
    s1.notifyStop(200, 200)
    stats.distribution(0) should be === 2L
    stats.distribution(1) should be === 2L
    stats.distribution(2) should be === 1L
    stats.distribution(3) should be === 0L
    
    s1.notifyStart(200)
    s1.notifyStop(500, 300)
    stats.distribution(0) should be === 2L
    stats.distribution(1) should be === 2L
    stats.distribution(2) should be === 1L
    stats.distribution(3) should be === 1L

    s1.notifyStart(200)
    s1.notifyStop(700, 500)
    stats.distribution(0) should be === 2L
    stats.distribution(1) should be === 2L
    stats.distribution(2) should be === 1L
    stats.distribution(3) should be === 1L
    stats.hitsUnderRange should be === 0
    stats.hitsOverRange should be === 1
  }
  
  test("Hits under range") {
    val factory = new StopwatchFactory
    factory.enabled = true
    factory.range = StopwatchRange(100, 400, 100)
    
    val s1 = factory.snapshot("foo").asInstanceOf[stopwatch.impl.StopwatchStatisticImpl]

    s1.distribution.length should be === 3
    s1.range.intervals should be === 3
    s1.range.interval(50) should be < 0
    s1.hitsUnderRange should be === 0
    
    s1.notifyStart(0)
    s1.notifyStop(50, 50)
    s1.hitsUnderRange should be === 1
  }
}
