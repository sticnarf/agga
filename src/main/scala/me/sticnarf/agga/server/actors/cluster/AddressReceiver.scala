package me.sticnarf.agga.server.actors.cluster

import akka.actor.{Actor, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.SendToAll
import me.sticnarf.agga.messages.{ServerInfo, ServerList}
import me.sticnarf.agga.server.messages.{RequestServer, ResponseServer}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

class AddressReceiver(val clientKey: String, val knownServers: Seq[String], val remote: ActorRef) extends Actor {
  private val mediator = DistributedPubSub(context.system).mediator
  private val timeout = 500 milliseconds
  private val servers = ArrayBuffer[ServerInfo]()

  override def preStart(): Unit = {
    mediator ! SendToAll("/user/addressProvider", RequestServer(clientKey))

    import scala.concurrent.ExecutionContext.Implicits.global
    context.system.scheduler.scheduleOnce(timeout, self, "timeout")
  }

  override def receive: Receive = {
    case ResponseServer(info) =>
      info.foreach(servers += _)

    case "timeout" =>
      remote ! ServerList(servers)
      context stop self
  }
}
