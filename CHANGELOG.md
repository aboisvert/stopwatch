CHANGELOG
=========

### v2.0.0 May 15, 2015

* Renamed artifact and packages to `stopwatch2` due to API changes & binary incompatibility.

* Removed StopwatchRange; distributions are now handled using percentiles (see `PSquared` class).

* All times are now expressed as `scala.concurrent.duration.Duration` instead of `long` values.

