package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging, Props}
import me.sticnarf.agga.server.messages.SendToClient

object Balancer {
  def props: Props = Props[Balancer]
}

class Balancer extends Actor with ActorLogging {
  val postman = context.actorSelection("/user/postman")

  override def receive: Receive = {
    case s@SendToClient(clientKey, _) =>
      // Send all segments to local postman
      postman ! s

    case x => log.error("Unknown message: {}", x)
  }
}
