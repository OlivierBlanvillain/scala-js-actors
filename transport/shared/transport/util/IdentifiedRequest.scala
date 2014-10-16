package transport.util

import scala.collection.mutable
import autowire._
import upickle._
import scala.concurrent._
import transport._
import autowire.Core.Request

/** Client side */
case class connectionSomethingClient(connection: Future[ConnectionHandle])(
      implicit ec: ExecutionContext) {
  
  private val pendingPromises = new PendingPromises[String]()
  
  connection.foreach { _.handlerPromise.success(
    new MessageListener {
      def notify(inboundPayload: String): Unit = {
        val identifiedResponse = upickle.read[IdentifiedResponse](inboundPayload)
        pendingPromises.get(identifiedResponse.id).success(identifiedResponse.res)
      }
    }
  )}
  
  def doCall(request: Request[String]): Future[String] = {
    val (id, future) = pendingPromises.next()
    connection.foreach { _.write(upickle.write(IdentifiedRequest(request, id))) }
    future
  }

}

/** */
class PendingPromises[T](implicit ec: ExecutionContext) {
  private var id = 0
  private val map = mutable.Map.empty[Int, Promise[T]]
  
  def next(): (Int, Future[T]) = {
    this.id = this.id + 1
    val newId = id
    val promise = Promise[T]()
    promise.future.onComplete { case _ => map.remove(newId) }
    map.update(newId, promise)
    (newId, promise.future)
  }

  def get(id: Int): Promise[T] = map(id)
}

/** Server side */
case class IdentifyingConnectionListener(actualCall: Request[String] => Future[String])(
      implicit ec: ExecutionContext) extends ConnectionListener {
  
  override def notify(connection: ConnectionHandle): Unit = connection.handlerPromise.success {
    new MessageListener {
      override def notify(pickle: String): Unit = {
        val identifiedRequest = upickle.read[IdentifiedRequest](pickle)
        val result: Future[String] = actualCall(identifiedRequest.req)
        result.foreach { response =>
          connection.write(upickle.write(IdentifiedResponse(response, identifiedRequest.id)))
        }
      }
    }
  }
  
}

case class IdentifiedRequest(req: Request[String], id: Int)

case class IdentifiedResponse(res: String, id: Int)
