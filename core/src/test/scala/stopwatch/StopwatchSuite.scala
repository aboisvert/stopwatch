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

import stopwatch.TimeUnit._

object StopwatchSuiteRunner {
  def main(args: Array[String]) = (new StopwatchSuite).execute
}

class StopwatchSuite extends FunSuite with ShouldMatchers {
  implicit def timeToNanos(t: TimeUnit) = t.toNanos

  test("Default statistic values") {
    val group = new StopwatchGroup("test")
    group.enabled = true
    val stat = group.snapshot("foo")
    stat.name should equal ("foo")
    stat.firstAccessTime should equal (None)
    stat.minTime should equal (-1)
    stat.averageTime should equal (0)
    stat.maxTime should equal (-1)
    stat.totalTime should equal (0)
    stat.lastAccessTime should equal (None)
  }

  test("Enabled group should call closure") {
    val group = new StopwatchGroup("test")
    group.enabled = true

    var called = false
    group("foo") {
      called = true
    }
    called should be === true
  }

  test("Disabled group should call closure") {
    val group = new StopwatchGroup("test")
    group.enabled = false

    var called = false
    group("foo") {
      called = true
    }
    called should be === true
  }

  test("Statistic values after one use") {
    val group = new StopwatchGroup("test")
    group.enabled = true

    val start = System.currentTimeMillis
    group("foo") {
      Thread.sleep(10)
    }
    val end = System.currentTimeMillis

    val stat = group.snapshot("foo")

    stat.name should equal ("foo")

    stat.firstAccessTime match {
      case Some(time) => time should ((be >= start) and (be <= end))
      case _ => fail
    }
    stat.averageTime should be >= (10 millis: Long)

    stat.standardDeviationTime should be === 0

    stat.range should be === null

    stat.distribution.length should be === 0

    stat.currentThreads should be === 0

    stat.averageThreads should be === 1

    stat.maxThreads should be === 1
  }

  test("Current threads, max threads and average threads") {
    val group = new StopwatchGroup("test")
    group.enabled = true

    val s1 = group.get("foo")
    val s2 = group.get("foo")
    val s3 = group.get("foo")

    group.snapshot("foo").currentThreads should be === 0
    s1.start()
    group.snapshot("foo").currentThreads should be === 1
    group.snapshot("foo").maxThreads     should be === 1
    s2.start()
    group.snapshot("foo").currentThreads should be === 2
    group.snapshot("foo").maxThreads     should be === 2
    s3.start()
    group.snapshot("foo").currentThreads should be === 3
    group.snapshot("foo").maxThreads     should be === 3
    Thread.sleep(10)
    s2.stop()
    group.snapshot("foo").currentThreads should be === 2
    group.snapshot("foo").maxThreads     should be === 3
    s1.stop()
    group.snapshot("foo").currentThreads should be === 1
    group.snapshot("foo").maxThreads     should be === 3
    s3.stop()
    group.snapshot("foo").currentThreads should be === 0
    group.snapshot("foo").maxThreads     should be === 3

    val stat = group.snapshot("foo")
    stat.averageThreads should be >= 1f
    stat.minTime should be >= 10L
    stat.averageTime should be >= 10L
    stat.maxTime should be >= 10L
    stat.totalTime should be >= 30L
  }

  test("Values after reset()") {
    val group = new StopwatchGroup("test")
    group.enabled = true
    group.range = StopwatchRange(0 millis, 200 millis, 100 millis)
    group("foo") {
      Thread.sleep(10)
    }
    group("foo") {
      Thread.sleep(100)
    }
    group.reset("foo")

    val stat = group.snapshot("foo")
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
    val group = new StopwatchGroup("test")
    group.enabled = true

    var called = false

    group.addListener { name: String =>
      name should be === "foo"
      called = true
    }

    group("foo") {
      called should be === false
    }
    called should be === true
  }

  test("error handling") {
    val group = new StopwatchGroup("test")
    group.enabled = true

    evaluating {
      group("foo") {
        error("yak!")
      }
    } should produce [RuntimeException]
  }
}
