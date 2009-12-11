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

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

object StopwatchSuiteRunner {
  def main(args: Array[String]) = (new StopwatchSuite).execute
}

class StopwatchSuite extends FunSuite with ShouldMatchers {

  test("Default statistic values") {
    val factory = new StopwatchFactory
    factory.enabled = true
    val stat = factory.snapshot("foo")
    stat.name should equal ("foo")
    stat.firstAccessTime should equal (None)
    stat.minTime should equal (-1)
    stat.averageTime should equal (0)
    stat.maxTime should equal (-1)
    stat.totalTime should equal (0)
    stat.lastAccessTime should equal (None)
  }

  test("Enabled factory should call closure") {
    val factory = new StopwatchFactory
    factory.enabled = true

    var called = false
    factory("foo") {
      called = true
    }
    called should be === true
  }
  
  test("Disabled factory should call closure") {
    val factory = new StopwatchFactory
    factory.enabled = false

    var called = false
    factory("foo") {
      called = true
    }
    called should be === true
  }
  
  test("Statistic values after one use") {
    val factory = new StopwatchFactory
    factory.enabled = true

    val start = System.currentTimeMillis
    factory("foo") {
      Thread.sleep(10)
    }
    val end = System.currentTimeMillis
    
    val stat = factory.snapshot("foo")

    stat.name should equal ("foo")
    
    stat.firstAccessTime match { 
      case Some(time) => time should ((be >= start) and (be <= end))
      case _ => fail
    }
    stat.averageTime should be >= 10f
    
    stat.standardDeviationTime should be === 0
    
    stat.range should be === null
    
    stat.distribution.length should be === 0
    
    stat.currentThreads should be === 0

    stat.averageThreads should be === 1

    stat.maxThreads should be === 1
  }
 
  test("Current threads, max threads and average threads") {
    val factory = new StopwatchFactory
    factory.enabled = true

    val s1 = factory.get("foo")
    val s2 = factory.get("foo")
    val s3 = factory.get("foo")
    
    factory.snapshot("foo").currentThreads should be === 0
    s1.start()
    factory.snapshot("foo").currentThreads should be === 1
    factory.snapshot("foo").maxThreads     should be === 1
    s2.start()
    factory.snapshot("foo").currentThreads should be === 2
    factory.snapshot("foo").maxThreads     should be === 2
    s3.start()
    factory.snapshot("foo").currentThreads should be === 3
    factory.snapshot("foo").maxThreads     should be === 3
    Thread.sleep(10)
    s2.stop()
    factory.snapshot("foo").currentThreads should be === 2
    factory.snapshot("foo").maxThreads     should be === 3
    s1.stop()
    factory.snapshot("foo").currentThreads should be === 1
    factory.snapshot("foo").maxThreads     should be === 3
    s3.stop()
    factory.snapshot("foo").currentThreads should be === 0
    factory.snapshot("foo").maxThreads     should be === 3

    val stat = factory.snapshot("foo")
    stat.averageThreads should be >= 1f
    stat.minTime should be >= 10L
    stat.averageTime should be >= 10f
    stat.maxTime should be >= 10L
    stat.totalTime should be >= 30L
  }

  test("Values after reset()") {
    val factory = new StopwatchFactory
    factory.enabled = true
    factory.range = StopwatchRange(0, 200, 100)
    factory("foo") {
      Thread.sleep(10)
    }
    factory("foo") {
      Thread.sleep(100)
    }
    factory.reset("foo")
    
    val stat = factory.snapshot("foo")
    stat.name should equal ("foo")
    stat.firstAccessTime should equal (None)
    stat.minTime should equal (-1)
    stat.averageTime should equal (0)
    stat.maxTime should equal (-1)
    stat.totalTime should equal (0)
    stat.lastAccessTime should equal (None)

    stat.standardDeviationTime should equal (0)
    stat.currentThreads should equal (0)
    stat.averageThreads should equal (0)
    stat.maxThreads should equal (0)
    stat.hits should equal (0)

    stat.distribution.forall { x => x == 0L }
  }

  test("Listeners") {
    val factory = new StopwatchFactory
    factory.enabled = true

    var called = false
    
    factory.addListener { name: String =>
      name should be === "foo"
      called = true
    }
    
    factory("foo") {
      called should be === false
    }
    called should be === true
  }
}
