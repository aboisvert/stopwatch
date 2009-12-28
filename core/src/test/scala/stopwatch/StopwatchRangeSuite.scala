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

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers

import stopwatch.TimeUnit._

@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class StopwatchRangeSuite extends WordSpec with ShouldMatchers {
  
  "StopwatchRange" when {
    
    "defined from 0 to 10 millis with 5 milliseconds intervals" should {
      val range = StopwatchRange(0 millis, 10 millis, 5 millis)
      
      "have 2 intervals" in {
        range.intervals should be === 2
      }

      "return 0 as interval for 1 millisecond" in {
        range.interval(1 * 1000000) should be === 0
      }
      
      "return 1 as interval for 5 millisecond" in {
        range.interval(5 * 1000000) should be === 1
      }
    }
  }
}
