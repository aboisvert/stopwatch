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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import scala.concurrent.duration._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class StopwatchPerformanceSuite extends FunSuite with ShouldMatchers {
  val disabled = new StopwatchGroup("disabled")

  val performance = {
    val perf = new StopwatchGroup("performance")
    perf.enabled = true
    perf
  }

  val enabled = {
    val group = new StopwatchGroup("enabled")
    group.enabled = true
    group
  }

  test("Performance of disabled stopwatch") {
    val n = 100
    val m = 1000000l
    var loops = 0
    while (loops < n) {
      performance("DisabledStopwatch") {
        var i = 0l
        while (i < m) {
          disabled("noop") { /* noop */ }
          i += 1
        }
      }
      loops += 1
      Thread.sleep(10)
      //Console println performance.snapshot("DisabledStopwatch")
      if (loops == n/2) performance.reset("DisabledStopwatch")
    }
    val s = performance.snapshot("DisabledStopwatch")
    Console println ("Disabled cost: %s".format(s.totalTime/(n*m)))
  }

  test("Performance of enabled stopwatch") {
    val n = 100
    val m = 10000l
    var loops = 0
    while (loops < n) {
      performance("EnabledStopwatch") {
        var i = 0l
        while (i < m) {
          enabled("noop") { /* noop */ }
          i += 1
        }
      }
      loops += 1
      Thread.sleep(10)
      // Console println performance.snapshot("EnabledStopwatch")
      if (loops == n/2) performance.reset("EnabledStopwatch")
    }
    val s = performance.snapshot("EnabledStopwatch")
    Console println ("Enabled cost: %s".format(s.totalTime/(n*m)))
  }
}
