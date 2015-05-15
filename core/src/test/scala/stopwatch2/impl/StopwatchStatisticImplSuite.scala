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

package stopwatch2.impl

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

import stopwatch2._
import scala.concurrent.duration._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class StopwatchStatisticImplSuite extends FunSuite with ShouldMatchers {

  test("Distribution intervals") {
    val factory = new StopwatchGroup("test")
    factory.enabled = true
    factory.percentiles = Array[Float](25, 50, 75)

    val s1 = factory.snapshot("foo").asInstanceOf[StopwatchStatisticImpl]

    s1.notifyStart(0)
    s1.notifyStop(50, 50 millis, false)

    var stats = s1
    stats.percentiles.length should be === 3
    stats.percentiles(0) shouldBe Percentile(25.0f, 50 millis)
    stats.percentiles(1) shouldBe Percentile(50.0f, 50 millis)
    stats.percentiles(2) shouldBe Percentile(75.0f, 50 millis)

    s1.notifyStart(0)
    s1.notifyStop(99, 99 millis, false)
    stats.percentiles(0) shouldBe Percentile(25.0f, 50 millis)
    stats.percentiles(1) shouldBe Percentile(50.0f, 99 millis)
    stats.percentiles(2) shouldBe Percentile(75.0f, 99 millis)

    s1.notifyStart(0)
    s1.notifyStop(100, 100 millis, false)
    stats.percentiles(0) shouldBe Percentile(25.0f, 50 millis)
    stats.percentiles(1) shouldBe Percentile(50.0f, 99 millis)
    stats.percentiles(2) shouldBe Percentile(75.0f, 100 millis)

    s1.notifyStart(0)
    s1.notifyStop(199, 199 millis, false)
    stats.percentiles(0) shouldBe Percentile(25.0f, 99 millis)
    stats.percentiles(1) shouldBe Percentile(50.0f, 100 millis)
    stats.percentiles(2) shouldBe Percentile(75.0f, 199 millis)

    s1.notifyStart(0)
    s1.notifyStop(200, 200 millis, false)
    stats.percentiles(0) shouldBe Percentile(25.0f, 99 millis)
    stats.percentiles(1) shouldBe Percentile(50.0f, 100 millis)
    stats.percentiles(2) shouldBe Percentile(75.0f, 199 millis)

    s1.notifyStart(200)
    s1.notifyStop(500, 300 millis, false)
    stats.percentiles(0) shouldBe Percentile(25.0f, 100 millis)
    stats.percentiles(1) shouldBe Percentile(50.0f, 100 millis)
    stats.percentiles(2) shouldBe Percentile(75.0f, 100 millis)
    stats.errors should be === 0

    s1.notifyStart(200)
    s1.notifyStop(700, 500 millis, true)
    stats.percentiles(0) shouldBe Percentile(25.0f, 100 millis)
    stats.percentiles(1) shouldBe Percentile(50.0f, 100 millis)
    stats.percentiles(2) shouldBe Percentile(75.0f, 155555552 nanos)
    stats.errors should be === 1
  }

}
