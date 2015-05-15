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

import java.io.{Console => _, _}

import java.util.concurrent.Executor

import java.net._

import java.text.DateFormat
import java.text.SimpleDateFormat

import stopwatch2._

/**
 * Small embedded webserver that implements a subset of HTTP 1.0 protocol
 * <p>
 * Server is single-threaded for simplicity and to limit resource consumption.
 */
class WebServer {
  import HttpUtils._

  /** Server daemon thread */
  @volatile private var _thread: Option[Thread] = None

  /** True if server is running */
  @volatile protected var _running = false

  /** Server socket */
  @volatile private var _serverSocket: Option[ServerSocket] = None

  /** HTTP port.  Default is 9999 */
  @volatile var port: Int = 9999

  /** Resource handler */
  @volatile var handlers: List[ResourceHandler] = List()

  protected val getRegex  = """GET\s+(\S+)\s*(\sHTTP/1\.[01])?\s*""".r
  protected val postRegex = """POST\s+(\S+)\s*(\sHTTP/1\.[01])?\s*""".r

  @volatile var log: Logger = NoLogger

  @volatile var debugLogging: Boolean = false

  @volatile var productionMode: Boolean = true

  @volatile var executor: Executor = SameThreadExecutor

  val stopwatch = new StopwatchGroup(getClass().getName)

  /** Start the server.  This method creates a new thread and returns. */
  def start() {
    if (_running)
      throw new IllegalStateException("Server is already running.")

    _running = true

    val serverLoop = new Runnable {
      def run = {
        log.info("Stopwatch server running")
        while (_running) {
          try {
            _serverSocket foreach { ss =>
              Console println ("accept")
              val socket = ss.accept()
              try {
                socket.setSoTimeout(30000)
                executor.execute( new Runnable {
                  def run = handleRequest(socket)
                })
              } finally {
                socket.close()
              }
            }
          } catch {
            case e: Throwable => e.printStackTrace
          }
        }
        log.info("Stopwatch server shutdown")
      }
    }

    _serverSocket = Some(new ServerSocket(port))

    _thread = {
      val t = new Thread(serverLoop, getClass().getName())
      t.setDaemon(true)
      t.start()
      Some(t)
    }
  }

  /** Stop the server */
  def stop() {
    _running = false
    _thread foreach (_.interrupt())
  }

  /** Handle HTTP request */
  private def handleRequest(socket: Socket) {
    implicit val in = new BufferedInputStream(socket.getInputStream)
    implicit val out = new BufferedOutputStream(socket.getOutputStream)

    try {
      val (request, response) = parseRequest(in)

      HttpContext._log.set(log)
      HttpContext._request.set(request)
      HttpContext._response set(response)

      stopwatch(request.path mkString "/") {
        handlers foreach { handler =>
          request.method match {
            case "GET" => handler.doGet(request, response)
            case "POST" => handler.doPost(request, response)
            case _ => log.error("Unexpected HTTP method: "+request.method)
          }
        }
        send(response)
      }
    } catch { case e: Throwable =>
      log.error(e.getMessage)
      if (!productionMode) e.printStackTrace()
      val response = new HttpResponse {
        var status = 500
        var headers = Map[String, String]()
      }
      send(response)
    } finally {
      ignore { out.flush() }
      ignore { in.close() }
      ignore { out.close() }
    }
  }

  private def ignore(f: => Unit) = try { f } catch { case e: Throwable => e.printStackTrace /* ignore */ }

  private def send(response: HttpResponse)(implicit out: OutputStream) {
    def writeLine(s: String) {
      log.debug(s)
      out write s.getBytes("UTF-8")
      out write '\r'
      out write '\n'
    }
    writeLine ("HTTP/1.0 "+response.status+"")
    // TODO send human-readable status?
    // out.write("HTTP/1.0 "+response.status+" OK")
    writeLine ("Server: stopwatch.web.WebServer/1.0")
    response.headers.foreach { header =>
      writeLine(header._1 + ": " + header._2)
    }
    response.contentLength foreach { length =>
      writeLine ("Content-Length: "+length.toString)
    }
    writeLine("")

    response.content foreach { bytes => out.write(bytes) }
    out.flush()
  }

  protected def parseRequest(in: InputStream): (HttpRequest, HttpResponse) = {
    var req = readLine(in)
    log.debug("Request: "+req)

    val (requestMethod, path) = req match {
      case getRegex(path, httpVersion) => ("GET", path)
      case postRegex(path, httpVersion) => ("POST", path)
      case _ => error("Illegal or unsupported request: "+req)
    }

    // separate path from query string
    // "/foo/bar?baz=1&quux=2" => "/foo/bar", "baz=1&quux=2"
    val (path2, query) = {
      (path split ("""\?""")).toList match {
        case List(path) => (path, "")
        case List(path, query) => (path, query)
        case _ => error("Illegal request path: "+req)
      }
    }

    // split path into path elements
    // "/foo/bar" => "foo", "bar"
    val path3 = (path2 split ("/") toList) filter (_.length != 0) map { _.trim } map { urlDecode }

    // prevent hijacking with relative path segments
    if (path3 exists { _ == ".." }) {
      error("Request path should not contain '..': "+req)
    }

    log.debug("Path: "+path3)

    // set extension
    val ext =  path3.lastOption map { extension(_) } getOrElse ""

    // parse query parameters
    val params: Map[String, String] = {
      var map = Map[String, String]()
      val list = query.split("&").toList.filter(_.length != 0)
      list foreach { param =>
        param.split("=").toList match {
          case List(name, value) if (name.length > 0) => map += (name -> value)
          case List(name) if (name.length > 0) => map += (name -> "")
          case _ =>
        }
      }
      map
    }

    // parse URL HTTP headers
    val requestHeaders = {
      var headers = Map[String, String]()
      var line = readLine(in)
      while (line != null && line.trim() != "") {
        val pos = line.indexOf(": ")
        if (pos > 0) {
          val header = line.substring(0,pos)
          val value = line.substring(pos+2, line.length)
          //log.debug("Header: '%s' -> '%s'".format(header, value))
          headers += (header -> value)
          line = readLine(in)
        } else {
          line = null
        }
      }
      headers
    }

    // read payload; assume content is 'application/x-www-form-urlencoded'
    var fields = List[(String, String)]()
    if (requestMethod == "POST") {
      val contentLength = (
        requestHeaders.get("Content-Length").map(_.toInt)
          getOrElse error("Content-Length required")
      )
      val buf = new Array[Byte](contentLength)
      var read = 0
      while (read < contentLength) {
        val n = in.read(buf, read, contentLength-read)
        read += n
      }
      val in2 = new ByteArrayInputStream(buf)
      var line = readLine(in2)
      while (line != null && line.trim.length > 0) {
        log.debug("Line: "+line)
        line.split("&").foreach { field =>
          field.split("=") match {
            case a if a.length == 2 =>
              fields = fields ::: List(urlDecode(a(0)) -> urlDecode(a(1)))
            case _ => log.debug("Ignored field: "+field)
          }
          line = readLine(in2)
        }
      }
      log.debug("Fields: "+fields)
    }

    val request = new HttpRequest {
      val method = requestMethod
      val path = path3
      val extension = ext
      val headers = requestHeaders
      val queryParams = params
      val formFields = fields
    }

    val response = new HttpResponse {
      var status = 500 // Internal error unless otherwise indicated
      var headers = Map[String, String]()
    }

    (request, response)
  }

  /** Extract file extension from a given path */
  private def extension(s: String): String = {
    val dot = s.lastIndexOf(".")
    val slash = s.lastIndexOf("/")
    if (dot > slash) s.substring(dot, s.length)
    else ""
  }

  object SameThreadExecutor extends Executor {
    def execute(r: Runnable) = r.run
  }
}

trait ResourceHandler {
  def doGet(request: HttpRequest, response: HttpResponse): Unit
  def doPost(request: HttpRequest, response: HttpResponse): Unit
}

object HttpContext {
  private[web] val _request  = new ThreadLocal[HttpRequest]
  private[web] val _response = new ThreadLocal[HttpResponse]
  private[web] val _log      = new ThreadLocal[Logger]

  def request: HttpRequest = _request.get
  def response: HttpResponse = _response.get
  def log: Logger = _log.get
}

trait HttpRequest {
  /** HTTP method:  "GET", "PUT", ... */
  val method: String

  /** Resource path (e.g. /foo/bar => List("foo", "bar") */
  val path: List[String]

  /** Query parameters
   *  e.g. /foo/bar?baz=1&quux=2 => Map("baz" -> 1, "quux" -> 2
   */
  val queryParams: Map[String, String]

  /** File extension; defined as all characters after the last dot of the last path.
   *  e.g. "/foo/bar.ext" => "ext"
   *       "/foo/bar.quux.ext => "ext"
   *       "/" => ""      (empty extension)
   *       "/foo." => ""  (empty extension)
   */
  val extension: String

  val formFields: Seq[(String, String)]
}

trait HttpResponse {
  private var _content: Option[Array[Byte]] = None

  /** HTTP status code (e.g. 200 OK) */
  var status: Int

  /** HTTP headers (e.g. "Content-Type" -> "text/html" */
  var headers: Map[String, String]

  def contentType = headers get "Content-Type"
  def contentType_=(s: String) { headers += ("Content-Type" -> s) }

  def content: Option[Array[Byte]] = _content
  def content_=(s: String) { _content = Some(s.getBytes("UTF-8")) }
  def content_=(bytes: Array[Byte]) { _content = Some(bytes) }

  def contentLength = _content map (_.length)
}

trait Logger {
  def debug(s: => String): Unit
  def info(s: String): Unit
  def warn(s: String): Unit
  def error(s: String) : Unit
}

object NoLogger extends Logger {
  def debug(s: => String) = ()
  def info(s: String) = ()
  def warn(s: String) = ()
  def error(s: String) = ()
}

/** Simple logger that writes to a given java.io.Writer */
class SimpleLogger(val writer: Writer) extends Logger {
  @volatile var logDebug = false
  @volatile var logInfo  = true
  @volatile var logWarn  = true
  @volatile var logError = true
  @volatile var logDateTime = true
  @volatile var logPrefix = ""
  @volatile var dateFormat: DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z")

  def this(out: OutputStream) = this(new OutputStreamWriter(out))
  def this() = this(System.out)

  override def debug(s: => String) = if (logDebug) log("DEBUG: "+s)
  override def info(s: String) =  if (logInfo)  log(" INFO: "+s)
  override def warn(s: String) =  if (logWarn)  log(" WARN: "+s)
  override def error(s: String) = if (logError) log("ERROR: "+s)

  protected def log(s: String) {
    import writer._
    if (logDateTime) {
      val now = System.currentTimeMillis
      val formatted = dateFormat.clone().asInstanceOf[DateFormat].format(now)
      write(formatted)
      write(" ")
    }
    if (logPrefix.length > 0) {
      write(logPrefix)
      write(" ")
    }
    write(s)
    write("\n")
    writer.flush()
  }

}

object HttpUtils {
  import HttpContext._

  /** Map of file extension to MIME types */
  @volatile var MIME_TYPES: Map[String, String] = {
    var map = Map[String, String]()
    map += ("html" -> "text/html")
    map += ("htm"  -> "text/html")
    map += ("css"  -> "text/css")
    map += ("gz"  -> "application/x-gzip")
    map += ("js"  -> "tesxt/javascript")
    map += ("gif"  -> "image/gif")
    map += ("jpeg" -> "image/jpeg")
    map += ("jpeg" -> "image/jpeg")
    map += ("png"  -> "image/png")
    map += ("txt"  -> "txt/plain")
    map
  }

  /** Send an error message with given HTTP error code */
  def sendError(code: Int, message: String) {
    val msg = (
      <html>
        <body>
          <p>{message}</p>
        </body>
      </html>
    )
    response.status = code
    response.contentType = "text/html"
    response.content = msg.toString
  }

  /** Redirect client to another location */
  def redirect(location: String) {
    log.info("Redirect: "+location)
    response.headers += ("Location" -> location)
    response.status = 307
  }

  /** Refer client to another location after POST */
  def seeOther(location: String) {
    log.info("See Other: "+location)
    response.headers += ("Location" -> location)
    response.status = 303
  }

  def readLine(in: InputStream): String = {
    val buf = new Array[Byte](4096)
    var i = 0

    def result = stripCRLF(new String(buf, 0, i, "UTF-8"))

    while (i < buf.length) {
      val x = in.read()
      if (x == -1 || x == '\n') {
        return result
      }
      buf(i) = x.toByte
      i += 1
    }
    result
  }

  private def stripCRLF(s: String) = {
    if (s.endsWith("\r")) s.substring(0, s.length-1)
    else if (s.endsWith("\r\n")) s.substring(0, s.length-2)
    else if (s.endsWith("\n")) s.substring(0, s.length-1)
    else s
  }

  def serveClasspathResource(path: String) {
    val resource = getResource(path)
    if (resource.isDefined) {
      val stream = new BufferedInputStream(resource.get.openStream())
      try {
        var path2 = path
        var headers = Map[String, String]()
        if (path2.endsWith(".gz")) {
          path2 = path.substring(0, path2.length-3)
          headers += ("Content-Encoding" -> "gzip")
        }
        val ext = extension(path2)
        val contentType = MIME_TYPES.get(ext) getOrElse "application/octet-stream"

        // TODO
        headers += ("Expires" -> "Thu, 01 Dec 2000 20:00:00 GMT")

        log.info("Serving: "+path+" ("+contentType+")")

        response.status = 200
        response.contentType = contentType
        response.headers ++= headers

        val out = new ByteArrayOutputStream()
        val buffer = new Array[Byte](4096)
        var bytesRead = stream.read(buffer)
        while (bytesRead >= 0) {
          out.write(buffer, 0, bytesRead)
          bytesRead = stream.read(buffer)
        }
        response.content = out.toByteArray
      } finally {
        stream.close()
      }
    } else {
      // The file/path was not found.
      sendError(404, "Resource not found.")
    }
  }

  def getResource(path: String): Option[URL] = {
    val url = getClass().getClassLoader().getResource(path);
    if (url ne null) Some(url) else None
  }

  def extension(s: String): String = {
    val dot = s.lastIndexOf(".")
    val slash = s.lastIndexOf("/")
    if (dot > slash) s.substring(dot+1, s.length)
    else ""
  }

  def urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

  def urlDecode(s: String) = URLDecoder.decode(s, "UTF-8")

  def id(s: String): String = s.replaceAll("[^a-zA-Z0-9]", "-");
}