package akka.scalajs.jsapi

import scala.scalajs.js

class WebSocket(url: String) extends js.Object with EventTarget {
  def send(message: String): Unit = js.native

  def close(code: Integer, reason: String): Unit = js.native
  def close(code: Integer): Unit = js.native
  def close(): Unit = js.native
}
