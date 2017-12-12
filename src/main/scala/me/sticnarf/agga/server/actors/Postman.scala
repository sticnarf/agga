package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Put, SendToAll}
import me.sticnarf.agga.messages.{Ack, ClientSegment, Connect}
import me.sticnarf.agga.server.AggaConfig
import me.sticnarf.agga.server.messages.{AckRegistry, RegisterClient, SendToClient}

import scala.collection.parallel.mutable.ParHashMap

object Postman {
  val clients = ParHashMap[String, Client]()

  def props(serverId: String): Props = Props(classOf[Postman], serverId, clients)
}

class Postman(val serverId: String, val clients: ParHashMap[String, Client]) extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  val mediator = DistributedPubSub(context.system).mediator

  mediator ! Put(self)

  val localAggregator = context.actorSelection("/user/aggregator")

  override def receive: Receive = {
    case Connect(key) =>
      log.info("Connection from {}", key)
      mediator ! SendToAll("/user/postman", RegisterClient(key))
      clients.put(key, Client(sender(), None))

    case RegisterClient(key) =>
      if (AggaConfig.acceptedKeys.contains(key)) {
        sender() ! AckRegistry(key, serverId)
      }

    case AckRegistry(clientKey, _) =>
      // TODO: Not thread safe
      clients.get(clientKey) match {
        case Some(Client(client, None)) =>
          client ! Ack(serverId)
          clients.put(clientKey, Client(client, Some(sender())))
      }

    case p@ClientSegment(conn, seq, data, clientKey) =>
      log.info("Received {} bytes", data.size())

      if (AggaConfig.acceptedKeys.contains(clientKey)) {
        localAggregator ! p
      } else {
        clients(clientKey).processor.foreach(_ ! p)
      }

    case s@SendToClient(clientKey, Some(data)) =>
      log.info("Send to client {} {} bytes", clientKey, data.data.size())
      clients(clientKey).client ! data
  }
}

case class Client(client: ActorRef, processor: Option[ActorRef])