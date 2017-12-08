package me.sticnarf.agga.server.actors

import akka.actor.Actor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Put
import me.sticnarf.agga.messages.ServerInfo
import me.sticnarf.agga.server.messages.{RequestServer, ResponseServer}

class AddressProvider(val uuid: String, val address: String) extends Actor {
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Put(self)

  override def receive: Receive = {
    case RequestServer(seq, clientKey) =>
      sender() ! ResponseServer(seq, Some(ServerInfo(uuid, address)))
  }
}
