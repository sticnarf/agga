package me.sticnarf.agga.server.actors

import akka.actor.Actor
import me.sticnarf.agga.messages.shared.{Connect, ServerInfo, ServerList}

class Navigator extends Actor {
  override def receive = {
    case c: Connect =>
      println(s"${c.key}: ${c.knownServers}")
      sender() ! ServerList(Seq(
        ServerInfo("1", "foobar"),
        ServerInfo("2", "bar"),
      ))
  }
}
