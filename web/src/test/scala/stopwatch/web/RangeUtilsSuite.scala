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

package stopwatch.web

import stopwatch.Stopwatch
import stopwatch.StopwatchGroup
import stopwatch.StopwatchRange

import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

object RangeUtilsSuiteRunner {
  def main(args: Array[String]) = (new RangeUtilsSuite).execute()
}

class RangeUtilsSuite extends FunSuite with ShouldMatchers {

  test("identity rescale") {
    val r1 = StopwatchRange(0, 100, 10)
    val r2 = StopwatchRange(0, 100, 10)
    val values = List[Long](0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    RangeUtils.rescale(r1, r2, values) should be === values
  }

  test("0-200:100 to 0-100:10") {
    val r1 = StopwatchRange(0, 200, 100)
    val r2 = StopwatchRange(0, 100, 10)
    val values = List[Long](1,2)
    RangeUtils.rescale(r1, r2, values) should be === (
      List(1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
    )
  }

  test("0:200:100 to 0:200:20") {
    val r1 = StopwatchRange(0, 200, 100)
    val r2 = StopwatchRange(0, 200, 20)
    val values = List[Long](1,2)
    RangeUtils.rescale(r1, r2, values) should be === (
      List(1, 1, 1, 1, 1, 2, 2, 2, 2, 2)
    )
  }

}