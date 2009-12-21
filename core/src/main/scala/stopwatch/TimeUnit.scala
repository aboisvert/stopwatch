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

/** Implicit conversion from Long to TimeUnit */
object TimeUnit {
  implicit def intToTimeUnitConverter(value: Int) = TimeUnitConverter(value)
  implicit def longToTimeUnitConverter(value: Long) = TimeUnitConverter(value)
}

/** Add "nanos", "micros", "millis" and "seconds" to Long numbers */
case class TimeUnitConverter(value: Long) {
  def nanos = TimeUnit(value, 1L)
  def micros = TimeUnit(value, 1000L)
  def millis = TimeUnit(value, 1000000L)
  def seconds = TimeUnit(value, 1000000000L)
}

/** Safe(r) abstraction for time units */
case class TimeUnit(value: Long, scale: Long) {
  private val nanos = value * scale
  def toNanos: Long = nanos
  def toMicros: Long = nanos / 1000L
  def toMillis: Long = nanos / 1000000L
  def toSeconds: Long = nanos / 1000000000L

  def +(other: TimeUnit) = TimeUnit(nanos+other.toNanos, 1)
  def -(other: TimeUnit) = TimeUnit(nanos-other.toNanos, 1)
  def *(other: TimeUnit) = TimeUnit(nanos*other.toNanos, 1)
  def /(other: TimeUnit) = TimeUnit(nanos/other.toNanos, 1)

  def +(other: Long) = TimeUnit(nanos+other, 1)
  def -(other: Long) = TimeUnit(nanos-other, 1)
  def *(other: Long) = TimeUnit(nanos*other, 1)
  def /(other: Long) = TimeUnit(nanos/other, 1)
}

