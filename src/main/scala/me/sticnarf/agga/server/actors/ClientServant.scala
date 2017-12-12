package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import me.sticnarf.agga.messages.ClientSegment
import me.sticnarf.agga.server.AggaConfig
import me.sticnarf.agga.server.messages.{AckRegistry, RegisterClient, SendToClient}

import scala.collection.mutable

class ClientServant(val key: String) extends Actor with ActorLogging {
  val redirectors = mutable.ArrayBuffer[ActorRef]()
  val aggregators = mutable.HashMap[Int, ActorRef]()

  var idx = 0

  override def receive: Receive = {
    case RegisterClient(_) =>
      redirectors += sender()
      sender() ! AckRegistry(key, AggaConfig.serverId)

    case p@ClientSegment(conn, _, _, clientKey) =>
      val aggregator = aggregators.getOrElseUpdate(conn, context.actorOf(Props(classOf[Aggregator], conn, clientKey, self)))
      log.info("Redirect to sub-aggregator {}", conn)
      aggregator ! p

    case s@SendToClient(clientKey, Some(data)) =>
      log.info("Send to client {} {} bytes", clientKey, data.data.size())
      val server = redirectors(idx % redirectors.size)
      idx += 1
      server ! s
  }
}
