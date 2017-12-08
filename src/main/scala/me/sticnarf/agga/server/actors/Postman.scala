package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import me.sticnarf.agga.messages.{Ack, ClientSegment, Connect}
import me.sticnarf.agga.server.messages.SendToClient

import scala.collection.mutable

object Postman {
  val clientActors = mutable.HashMap[String, ActorRef]()

  def props(serverId: String): Props = Props(classOf[Postman], serverId, clientActors)
}

class Postman(val serverId: String, val clientActors: mutable.HashMap[String, ActorRef]) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Connect(key) =>
      log.info("Connection from {}", key)
      sender() ! Ack(serverId)
      clientActors.put(key, sender())

    case p@ClientSegment(conn, seq, data, clientKey) =>
      log.info("Received {} bytes", data.size())

      // Temporarily use local aggregator
      val aggregator = context.actorSelection("/user/aggregator")
      aggregator forward p

    case s@SendToClient(clientKey, Some(data)) =>
      log.info("Send to client {} {} bytes", clientKey, data.data.size())
      clientActors(clientKey) ! data
  }
}
