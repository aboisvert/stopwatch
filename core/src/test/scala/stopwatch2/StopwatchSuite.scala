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
import org.scalatest._

import scala.concurrent.duration._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class StopwatchSuite extends FunSuite with Matchers {

  val i = implicitly[Ordering[Duration]]

  test("Default statistic values") {
    val group = new StopwatchGroup("test")
    group.enabled = true
    val stat = group.snapshot("foo")
    stat.name should equal ("foo")
    stat.firstAccessTime should equal (None)
    stat.minTime shouldBe Duration.Undefined
    stat.averageTime shouldBe Duration.Undefined
    stat.maxTime shouldBe Duration.Undefined
    stat.totalTime shouldBe Duration.Zero
    stat.lastAccessTime should equal (None)
  }

  test("Enabled group should call closure") {
    val group = new StopwatchGroup("test")
    group.enabled = true

    var called = false
    group("foo") {
      called = true
    }
    called shouldBe true
  }

  test("Disabled group should call closure") {
    val group = new StopwatchGroup("test")
    group.enabled = false

    var called = false
    group("foo") {
      called = true
    }
    called shouldBe true
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

    stat.averageTime.toMillis should (be >= 10L)

    stat.standardDeviationTime shouldBe Duration.Zero

    stat.percentiles.length shouldBe 13

    stat.currentThreads shouldBe 0

    stat.averageThreads shouldBe 1

    stat.maxThreads shouldBe 1
  }

  test("Current threads, max threads and average threads") {
    val group = new StopwatchGroup("test")
    group.enabled = true

    val s1 = group.get("foo")
    val s2 = group.get("foo")
    val s3 = group.get("foo")

    group.snapshot("foo").currentThreads shouldBe 0
    s1.start()
    group.snapshot("foo").currentThreads shouldBe 1
    group.snapshot("foo").maxThreads     shouldBe 1
    s2.start()
    group.snapshot("foo").currentThreads shouldBe 2
    group.snapshot("foo").maxThreads     shouldBe 2
    s3.start()
    group.snapshot("foo").currentThreads shouldBe 3
    group.snapshot("foo").maxThreads     shouldBe 3
    Thread.sleep(10)
    s2.stop()
    group.snapshot("foo").currentThreads shouldBe 2
    group.snapshot("foo").maxThreads     shouldBe 3
    s1.stop()
    group.snapshot("foo").currentThreads shouldBe 1
    group.snapshot("foo").maxThreads     shouldBe 3
    s3.stop()
    group.snapshot("foo").currentThreads shouldBe 0
    group.snapshot("foo").maxThreads     shouldBe 3

    val stat = group.snapshot("foo")
    stat.averageThreads shouldBe >= (1.0f)
    stat.minTime.toNanos shouldBe >= (10L)
    stat.averageTime.toNanos shouldBe >= (10L)
    stat.maxTime.toNanos shouldBe >= (10L)
    stat.totalTime.toNanos shouldBe >= (30L)
  }

  test("Values after reset()") {
    val group = new StopwatchGroup("test")
    group.enabled = true
    group.percentiles = Array[Float](50)
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
    stat.minTime shouldBe Duration.Undefined
    stat.averageTime shouldBe Duration.Undefined
    stat.maxTime shouldBe Duration.Undefined
    stat.totalTime shouldBe Duration.Zero
    stat.lastAccessTime should equal (None)

    stat.standardDeviationTime shouldBe Duration.Undefined
    stat.currentThreads should equal (0)
    stat.averageThreads should equal (0)
    stat.maxThreads should equal (0)
    stat.hits should equal (0)

    stat.percentiles shouldBe Seq(Percentile(50.0f, Duration.Zero))
  }

  test("Listeners") {
    val group = new StopwatchGroup("test")
    group.enabled = true

    var called = false

    group.addListener { snapshot =>
      snapshot.name shouldBe "foo"
      called = true
    }

    group("foo") {
      called shouldBe false
    }
    called shouldBe true
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
