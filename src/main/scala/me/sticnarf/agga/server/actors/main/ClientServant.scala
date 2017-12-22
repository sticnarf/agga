package me.sticnarf.agga.server.actors.main

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.ConnectionClosed
import me.sticnarf.agga.messages.{ClientSegment, ServerSegment}
import me.sticnarf.agga.server.{AggaConfig, ServerMain}
import me.sticnarf.agga.server.messages.{AckRegistry, RegisterClient, SendToClient}

import scala.collection.mutable

class ClientServant(val key: String) extends Actor with ActorLogging {
  private val redirectors = mutable.ArrayBuffer[ActorRef]()
  private val aggregators = mutable.HashMap[Int, ActorRef]()
  private val postman = ServerMain.clusterSystem.actorSelection("/user/postman")

  object sampleServer extends (() => ActorRef) {
    private var idx = 0;

    def apply: ActorRef = {
      val server = redirectors(idx % redirectors.size)
      idx += 1
      server
    }
  }

  override def receive: Receive = {
    case (RegisterClient(_), redirector: ActorRef) =>
      redirectors += redirector // Add helper's Postman to redirector list
      postman ! (AckRegistry(key, AggaConfig.serverId), redirector)

    case p@ClientSegment(conn, seq, data, clientKey) =>
      if (seq == -1 && data.isEmpty) {
        aggregators.get(conn).foreach(_ ! "close")
      } else {
        val aggregator = aggregators.getOrElseUpdate(conn, context.actorOf(Props(classOf[Aggregator], conn, clientKey, self)))
        log.info("Redirect to sub-aggregator {}", conn)
        aggregator ! p
      }

    case s@SendToClient(clientKey, Some(data)) =>
      postman ! (s, sampleServer())

    case (_: ConnectionClosed, conn: Int) =>
      postman ! (ServerSegment(conn, -1, com.google.protobuf.ByteString.EMPTY), sampleServer())
      aggregators -= conn
  }
}
