package me.sticnarf.agga.server.actors

import akka.actor.Actor

class SocksServer extends Actor {
  override def receive: Receive = {
    case x => println(x)
  }
}
