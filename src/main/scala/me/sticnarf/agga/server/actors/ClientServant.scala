package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.ConnectionClosed
import me.sticnarf.agga.messages.{ClientSegment, ServerSegment}
import me.sticnarf.agga.server.AggaConfig
import me.sticnarf.agga.server.messages.{AckRegistry, RegisterClient, SendToClient}

import scala.collection.mutable
import scalapb.descriptors.ScalaType.ByteString

class ClientServant(val key: String) extends Actor with ActorLogging {
  val redirectors = mutable.ArrayBuffer[ActorRef]()
  val aggregators = mutable.HashMap[Int, ActorRef]()

  object sampleServer extends (() => ActorRef) {
    private var idx = 0;

    def apply: ActorRef = {
      val server = redirectors(idx % redirectors.size)
      idx += 1
      server
    }
  }

  override def receive: Receive = {
    case RegisterClient(_) =>
      redirectors += sender()
      sender() ! AckRegistry(key, AggaConfig.serverId)

    case p@ClientSegment(conn, seq, data, clientKey) =>
      if (seq == -1 && data.isEmpty) {
        aggregators.get(conn).foreach(_ ! "close")
      } else {
        val aggregator = aggregators.getOrElseUpdate(conn, context.actorOf(Props(classOf[Aggregator], conn, clientKey, self)))
        log.info("Redirect to sub-aggregator {}", conn)
        aggregator ! p
      }

    case s@SendToClient(clientKey, Some(data)) =>
      log.info("Send to client {} {} bytes", clientKey, data.data.size())
      sampleServer() ! s

    case (_: ConnectionClosed, conn: Int) =>
      sampleServer() ! ServerSegment(conn, -1, com.google.protobuf.ByteString.EMPTY)
      aggregators -= conn
  }
}
