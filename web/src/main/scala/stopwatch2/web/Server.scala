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

package stopwatch2.web

import stopwatch2._

import java.io._
import java.net._

import scala.xml._

/**
 * Embedded stopwatch statistics web server
 */
class Server extends WebServer with ResourceHandler {
  import HttpUtils._

  handlers = List(this)

  var groups = List[StopwatchGroup]()

  /** Encode identifiers for use in attributes */
  private def encode(s: String) = urlEncode(s).replaceAll("/", "%2F")

  override def doGet(request: HttpRequest, response: HttpResponse) {
    def resource(f: PartialFunction[List[String], String]) = {
      if (f isDefinedAt request.path) Some(f(request.path))
      else None
    }

    // default to index.html
    request.path match {
      case List() | List("index.htm") | List("index.html") =>
        serveStopwatches()
      case List("distribution", group, stopwatch) =>
        distribution(group, stopwatch)
      case _ =>
        val resource = "stopwatch/web/static/" + (request.path mkString "/")
        if (getResource(resource).isDefined) {
          serveClasspathResource(resource)
        } else {
          response.status = 404
          response.content = "Resource not found."
        }
    }
  }

  override def doPost(request: HttpRequest, response: HttpResponse) {
    def field(s: String) = request.formFields find (_._1 == s) map (_._2)

    def resource(f: PartialFunction[List[String], String]) = {
      if (f isDefinedAt request.path) Some(f(request.path))
      else None
    }

    val group = resource { case List("group", name, _*) => name }
    val stopwatch = resource { case List("group", _, "stopwatch", name) => name }
    val action = field("action")

    action match {
      case Some("enableStopwatch") =>
        for (g <- group; s <- stopwatch) {
          log.debug("enableStopwatch: %s %s" format (g,s))
          groups filter (_.name == g) foreach (_ enable s)
        }
        seeOther("/")

      case Some("disableStopwatch") =>
        for (g <- group; s <- stopwatch) {
          log.debug("disableStopwatch: %s %s" format (g,s))
          groups filter (_.name == g) foreach (_ disable s)
        }
        seeOther("/")

      case Some("enableGroup") =>
        log.debug("enableGroup: %s" format group)
        for (g <- group) { groups filter (_.name == g) foreach (_ enabled = true) }
        seeOther("/")

      case Some("disableGroup") =>
        log.debug("disableGroup: %s" format group)
        for (g <- group) { groups filter (_.name == g) foreach (_ enabled = false) }
        seeOther("/")

      case Some("reset") =>
        for (g <- group; s <- stopwatch) {
          log.debug("resetStopwatch: %s %s" format (g,s))
          groups filter (_.name == g) foreach (_ reset s)
        }
        seeOther("/")

      case Some("resetGroup") =>
        for (g <- group) {
          log.debug("resetGroup: %s" format (g))
          groups filter (_.name == g) foreach (_ resetAll)
        }
        seeOther("/")

      case Some(unknown) =>
        sendError(500, "Unknown action: " + unknown)

      case None =>
        sendError(500, "Missing action")
    }
  }

  def serveStopwatches() {
    import HttpContext.{request, response}

    response.status = 200
    response.contentType = "text/html"

    val sortedGroups  = groups.toList sortWith (_.name < _.name)

    val xhtml = <html>
      <head>
        <title>Stopwatch</title>
        <link href="/css/reset.css" rel="stylesheet" type="text/css" />
        <link href="/css/stopwatch.css" rel="stylesheet" type="text/css" />
        <!-- <link href="/css/jquery-checkbox.css" rel="stylesheet" type="text/css" /> -->
        <script type="text/javascript" src="/js/jquery.js"></script>
        <!-- <script type="text/javascript" src="/js/jquery-checkbox.js"></script> -->
        <script type="text/javascript" src="/js/jquery-colorize.js"></script>
        <script type="text/javascript" src="/js/jquery-sparkline.js"></script>
        <script type="text/javascript" src="/js/jquery-timers.js"></script>
        <!-- <script type="text/javascript" src="/js/jquery-tools-tabs.js"></script> -->
        <script> { Unparsed("""
        function applyStyles() {
          // apply jQuery table colorize styling
          $('.stopwatches').colorize();

          // setup ul.tabs to work as tabs for each div directly under div.panes
          //$('ul.tabs').tabs('div.panes > div', {effect: 'fade', fadeOutSpeed: 400});

          // Inline sparklines take their values from the contents of the tag
          $('.inlinesparkline').sparkline('html', {type: 'bar'});

          // apply jQuery checkbox styling
          //$('input:checkbox').checkbox();

          // stopwatch group on/off switches
          $('.groupSwitch input:checkbox').bind('click', function(e) {
            var action = this.checked ? 'disableGroup' : 'enableGroup'
            var group = this.name
            jQuery.post('/group/'+group, {action: action});
          });

          // individual stopwatch on/off switches
          $('.stopwatchSwitch input:checkbox').bind('click', function(e) {
            var action = this.checked == true ? 'enableStopwatch' : 'disableStopwatch'
            var re = new RegExp('(.*)~(.*)');
            var m = re.exec(this.name);
            var group = m[1];
            var stopwatch = m[2];
            // alert('click '+action+' '+group+' '+stopwatch);
            jQuery.post('/group/'+group+'/stopwatch/'+stopwatch, {action: action});
          });
        }

        function refreshStopwatches() {
          $("#stopwatches").load("/ #stopwatches", function() {
            applyStyles();
          });
        }

        $(document).ready(function() { applyStyles(); } );

        $(document).everyTime(5000, function(i) {
          /* group tabs don't work yet
          alert("load");
          $.get("/", function(data) {
            $(".groupReplace").each(function (d) {
              alert("data "+data)
              alert("source "+this.id)
              alert("replace "+$("#"+this.id, data).id)
              $(d).insertAfter( $("#"+this.id, data) )
              $(d).replaceWith( $("#"+this.id, data) );
            });
          }, "html");
          $(".groupReplace").each(function (d) {
            $(this).load("/ #"+this.id, function() {
              applyStyles();
            });
          });
          */
          refreshStopwatches();
        });
         """) }
        </script>
      </head>
      <body> {
        <div id="content">

          <div id="header">
            <div>
              <span id="title">Stopwatches</span>
            </div>
          </div>

          <div id="stopwatches"> {
            <div/> ++
            /* group tabs don't work yet
            <ul class="tabs"> { sortedGroups map { g =>
              <li><a href="#">{g.name}</a></li>
            } } </ul>
            */
            <div class="panes"> {
              sortedGroups map { renderGroup(_) }
            } </div>
          } </div>

          <div id="footer" class="last">
            <br/>
            <a href="http://github.com/aboisvert/stopwatch">Stopwatch</a> is Copyright (C) 2009-2010, by Alex Boisvert
            <br/>
          </div>

        </div>
      } </body>
    </html>
    response.content = xhtml.toString
  }

  def headers(g: StopwatchGroup): NodeSeq = {
    <thead>
      <tr> {
        <th scope="row" class="lead" rowspan="2">Name</th> ++
        <th scope="col" colspan="3">Hits</th> ++
        <th scope="col" colspan="5">Time</th> ++
        <th scope="col" colspan="3">Threads</th> ++
        <th scope="col" colspan="2">Access</th> ++ {
          if (g.percentiles ne null) {
            <th scope="col" rowspan="2">Percentiles</th>
          } else NodeSeq.Empty
        } ++
        <th scope="col" rowspan="2">Enabled</th>
        <th scope="col" rowspan="2">Action</th>
      } </tr>
      <tr>
        <th scope="col" class="odd">Total</th>
        <th scope="col">Hits/s</th>
        <th scope="col">Errors</th>
        <th scope="col" class="odd">Min</th>
        <th scope="col">Avg</th>
        <th scope="col" class="odd">Max</th>
        <th scope="col">Total</th>
        <th scope="col" class="odd">Std Dev</th>
        <th scope="col">Current</th>
        <th scope="col" class="odd">Avg</th>
        <th scope="col">Max</th>
        <th scope="col" class="odd">First</th>
        <th scope="col">Last</th>
      </tr>
    </thead>
  }

  def renderGroup(g: StopwatchGroup) = {
    val reset = (
        "jQuery.post('/group/%s', {action: 'resetGroup'}); refreshStopwatches(); return false;"
        .format(encode(g.name))
    )

    <div class="groupReplace" id={id(g.name)}>
      <h3 class="StopwatchGroup"> {g.name} </h3>
      <span class="groupSwitch">Enabled&nbsp;
        <input type="checkbox" name={g.name}
               checked={if (g.enabled) "checked" else null}/>
      </span>
      <span class="groupReset">
        <a class="resetGroup" href="#" onclick={reset}>Reset Group</a>
      </span>
      <table class="stopwatches">
        { headers(g) ++ <tbody> { rows(g) } </tbody> }
      </table>
    </div>
  }

  def rows(group: StopwatchGroup) = {
    var i = 1
    group.names.toList.sortWith(_ < _).map { name: String =>
      val snapshot = group.snapshot(name)
      i += 1
      row(i, group, snapshot)
    }
  }

  def row(i: Int, g: StopwatchGroup, s: StopwatchStatistic) = {
    val dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
    val delta = for {
      last <- s.lastAccessTime
      first <- s.firstAccessTime
    } yield last-first

    val throughput = {
      val t = for (d <- delta) yield (s.hits * 1000 / (d: Float))
      t map ("%3.4f".format(_)) getOrElse "N/A"
    }

    def formatTime(time: Option[Long]) = {
      time.map(dateFormat.format(_)) getOrElse "N/A"
    }

    val millis = 1000000

    val reset = (
        "jQuery.post('/group/%s/stopwatch/%s', {action: 'reset'}); refreshStopwatches(); return false;"
        .format(encode(g.name), encode(s.name))
    )

    <tr class={if (i%2==1) "odd" else "even"}> {
      <th scope="row">{s.name}</th> ++
      <td>{s.hits}</td> ++
      <td>{throughput}</td> ++
      <td>{s.errors}</td> ++
      <td>{s.minTime/millis}</td> ++
      <td>{s.averageTime/millis}</td> ++
      <td>{s.maxTime/millis}</td> ++
      <td>{s.totalTime/millis}</td> ++
      <td>{s.standardDeviationTime/millis}</td> ++
      <td>{s.currentThreads}</td> ++
      <td>{s.averageThreads}</td> ++
      <td>{s.maxThreads}</td> ++
      <td>{formatTime(s.firstAccessTime)}</td> ++
      <td>{formatTime(s.lastAccessTime)}</td> ++ {
        /*

        if (g.percentiles.nonEmpty) {
          <td><a href={"/distribution/%s/%s" format (encode(g.name), encode(s.name))}>
            <span class="inlinesparkline"> {
              val newRange = {
                val low = s.range.lowerBound
                val high = s.range.higherBound
                StopwatchRange(low, high, (high-low)/20)
              }
              val newDist = (
                s.hitsUnderRange ::
                RangeUtils.rescale(s.range, newRange, s.distribution)
                ::: List(s.hitsOverRange)
              )
              newDist mkString ","
            } </span>
          </a></td>
        } else
        */

        NodeSeq.Empty } ++
      <td>
        <div class="stopwatchSwitch">
          <input type="checkbox" name={g.name+"~"+s.name}
                 checked={if (s.enabled) "checked" else null}/>
        </div>
      </td> ++
      <td>
        <a class="resetStopwatch" href="#" onclick={reset}>Reset</a>
      </td>
    } </tr>
  }

  def distribution(group: String, stopwatch: String) {
    import HttpContext.{request, response}

    response.status = 200
    response.contentType = "text/html"

    groups.filter(_.name == group).headOption foreach { group =>
      val s = group.snapshot(stopwatch)

      import s._

      /*
      val step: Long = (range.higherBound - range.lowerBound) / 10
      val xmin: Long = range.lowerBound - step
      val xmax: Long = range.higherBound + step
      val data = {
        // var data = [[0,5],[1,2], [2,3], [3,4], [4,5]];
        List((xmin, s.hitsUnderRange)) :::
        ( s.distribution.toList.zipWithIndex
            map { case (hits, i) => ((range.lowerBound+(i*range.step)) toNanos, hits) }
        ) ::: List((xmax, s.hitsOverRange)) map {
          case (hits, x) => "[%d,%d]".format(hits, x)
        } mkString ","
      }
      val convert = if (step.nanos > 1000000) {
        (x: Long) => (x/1000000).toString + "ms"
      } else {
        (x: Long) => (x/1000000).toString + "ns"
      }
      val xticks = {
        val r = StopwatchRange(xmin nanos, xmax nanos, step nanos).toList
        // var ticks = [[0,"Under"],[100,11], [200,22], [300,33], [400,"Over"]];
        List((xmin, "Under")) :::
        r.toList.map { x => (x, convert(x)) } :::
        List((xmax, "Over")) map {
          case (x, s) => """[%d,"%s"]""".format(x,s)
        } mkString ","
      }
      val ymin = 0
      val ymax = ((s.distribution.reduceRight((a:Long, b:Long) => a.max(b))+10)/10*10) // we want multiple of 10
      val yticks = StopwatchRange(0 nanos, ymax nanos, (ymax/10) nanos).toList mkString ("[", ",", "]")

      */

      val xhtml = <html>
        <head>
          <title>{"Distribution: "+stopwatch}</title>
          <link href="/css/reset.css" rel="stylesheet" type="text/css" />
          <link href="/css/stopwatch.css" rel="stylesheet" type="text/css" />
          <link href="/css/jquery-checkbox.css" rel="stylesheet" type="text/css" />
          <script type="text/javascript" src="/js/jquery.js"></script>
          <script type="text/javascript" src="/js/jquery-flot.js"></script>
          <script type="text/javascript" src="/js/jquery-timers.js"></script>
          <script> { Unparsed("""
           function applyStyles() {
              $.plot($("#placeholder"), [
              {
                label: "Hits / Interval",
                data: [$data],
                bars: { show: true }
              }], {
                lines: { fill: true },
                xaxis: {
                  ticks: [$xticks],
                  min: $xmin,
                  max: $xmax
                },
                yaxis: {
                    ticks: $yticks,
                    min: $ymin,
                    max: $ymax
                },
              });
            }

            $(document).ready(function() { applyStyles(); } );

            /*
            $(document).everyTime(5000, function(i) {
              $("#stopwatches").load("/ #stopwatches", function() {
                applyStyles();
              });
            });
            */
          """) }
          </script>
        </head>
        <body>
          <div id="content">
            <h1>{stopwatch}</h1>
            <div id="placeholder" style="width:800px;height:400px"/>
          </div>
        </body>
      </html>


      val result = ( xhtml.toString
          /*
        .replaceFirst("""\$data""", data)
        .replaceFirst("""\$xticks""", xticks)
        .replaceFirst("""\$xmin""", xmin.toString)
        .replaceFirst("""\$xmax""", xmax.toString)
        .replaceFirst("""\$yticks""", yticks)
        .replaceFirst("""\$ymin""", ymin.toString)
        .replaceFirst("""\$ymax""", ymax.toString)
        */
      )

      response.content = result

    }
  }
}
