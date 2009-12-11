Stopwatch
=========

#### Stopwatch API to easily monitor production Scala applications. ####

This API can be used to determine application performance bottlenecks,
user/application interactions, and application scalability. Each stopwatch
gathers summary statistics such as hits, execution times (total, average,
minimum, maximum, standard deviation), distribution, and simultaneous
application requests.

### Basic Usage ###
    
    import stopwatch.Stopwatch

    Stopwatch("test") {
      // code being timed
    }
   
### Usage for non-inlined code ###

    val stopwatch = Stopwatch.start("test")

    // and later, elsewhere in your code
    stopwatch.stop()
    
### Defining distribution intervals ###

    import stopwatch.Stopwatch

    // somewhere in your code, before using the stopwatch
    // distribution:  1s to 10s in 1s intervals
    Stopwatch.init("test", 1000 to 10000 step 1000)

### Displaying statistics ###
    
    stats = Stopwatch.snapshot("test")
    Console.println(stats.toShortString)
    Console.println(stats.toMediumString) // includes thread info
    Console.println(stats.toLongString) // includes threads + hit distribution
    Console.println(stats) // defaults to medium string
    
### Target platform ###

* Scala 2.7.4+ / 2.8+  (source compatible)
* JVM 1.5+

### License ###

Copyright (C) 2009-2010 by Alex Boisvert.

Stopwatch is is licensed under the terms of the Apache Software License v2.0. 
<http://www.apache.org/licenses/LICENSE-2.0.html>

