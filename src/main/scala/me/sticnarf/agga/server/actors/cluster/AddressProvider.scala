package me.sticnarf.agga.server.actors.cluster

import akka.actor.Actor
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Put
import me.sticnarf.agga.messages.ServerInfo
import me.sticnarf.agga.server.messages.{RequestServer, ResponseServer}
import me.sticnarf.agga.server.{AggaConfig, ServerMain}

class AddressProvider extends Actor {
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Put(self)

  override def receive: Receive = {
    case RequestServer(clientKey) =>
      sender() ! ResponseServer(Some(
        ServerInfo(
          AggaConfig.serverId,
          ServerMain.address.toString
        )
      ))
  }
}
