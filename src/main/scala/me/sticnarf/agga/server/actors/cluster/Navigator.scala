package me.sticnarf.agga.server.actors.cluster

import akka.actor.{Actor, ActorLogging, Props}
import me.sticnarf.agga.messages.FetchServerList

class Navigator extends Actor with ActorLogging {
  override def receive = {
    case FetchServerList(clientKey, knownServers) =>
      log.debug("Receive connect request. Key: {}, Known servers: {}", clientKey, knownServers)
      context.actorOf(Props(classOf[AddressReceiver], clientKey, knownServers, sender()))
  }
}
