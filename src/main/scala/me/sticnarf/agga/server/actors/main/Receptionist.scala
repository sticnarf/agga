package me.sticnarf.agga.server.actors.main

import akka.actor.{Actor, Props}
import me.sticnarf.agga.messages.FetchServerList
import me.sticnarf.agga.server.ServerMain
import me.sticnarf.agga.server.actors.cluster.AddressReceiver

class Receptionist extends Actor {
  override def receive: Receive = {
    case FetchServerList(key, knownServers) => {
      ServerMain.clusterSystem.actorOf(Props(classOf[AddressReceiver],key, knownServers, sender()))
    }
  }
}
