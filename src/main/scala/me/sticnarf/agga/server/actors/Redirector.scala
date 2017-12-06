package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging}
import me.sticnarf.agga.messages.Packet

class Redirector extends Actor with ActorLogging {
  override def receive: Receive = {
    case Packet(conn, seq, data) =>
      log.info("Received {} bytes", data.size())
  }
}
