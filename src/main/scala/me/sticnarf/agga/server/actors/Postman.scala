package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Put, SendToAll}
import me.sticnarf.agga.messages.{Ack, ClientSegment, Connect}
import me.sticnarf.agga.server.AggaConfig
import me.sticnarf.agga.server.messages.{AckRegistry, RegisterClient, SendToClient}

import scala.collection.mutable

object Postman {
  val remotes = mutable.HashMap[String, ActorRef]()
  val clients = mutable.HashMap[String, ActorRef]()

  def props: Props = Props(classOf[Postman], remotes, clients)
}

class Postman(val remotes: mutable.HashMap[String, ActorRef],
              val clients: mutable.HashMap[String, ActorRef]) extends Actor with ActorLogging {
  //  val cluster = Cluster(context.system)
  private val mediator = DistributedPubSub(context.system).mediator

  mediator ! Put(self)

  override def receive: Receive = {
    case Connect(key) =>
      log.info("Connection from {}", key)
      remotes.put(key, sender())
      mediator ! SendToAll("/user/postman", RegisterClient(key))

    case reg@RegisterClient(key) =>
      log.info("Receive RegisterClient from {}", sender())
      if (AggaConfig.acceptedKeys.contains(key)) {
        val servant = clients.getOrElseUpdate(key, context.actorOf(Props(classOf[ClientServant], key)))
        servant forward reg
      }

    case ack@AckRegistry(clientKey, serverId) =>
      log.info("AckRegistry from server {}", serverId)
      clients.put(clientKey, sender())
      remotes.get(clientKey).foreach(_ ! Ack(AggaConfig.serverId))

    case p@ClientSegment(conn, seq, data, clientKey) =>
      log.info("Received {} bytes", data.size())
      clients(clientKey) ! p

    case s@SendToClient(clientKey, Some(data)) =>
      log.info("Send segment: conn {}, seq {}, size {}", data.conn, data.seq, data.data.size())
      remotes(clientKey) ! data
  }
}