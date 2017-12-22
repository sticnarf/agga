package me.sticnarf.agga.server.actors.cluster

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Put, SendToAll}
import me.sticnarf.agga.messages.{ClientSegment, Connect, ServerSegment}
import me.sticnarf.agga.server.ServerMain
import me.sticnarf.agga.server.messages.{AckRegistry, RegisterClient, SendToClient}

class Postman extends Actor with ActorLogging {
  private val server = ServerMain.aggaSystem.actorSelection("/user/server")
  private val mediator = DistributedPubSub(context.system).mediator

  mediator ! Put(self)

  override def receive: Receive = {
    case Connect(key) =>
      mediator ! SendToAll("/user/postman", RegisterClient(key))

    case (ack@AckRegistry(_, _), redirector: ActorRef) =>
      redirector ! ack // Send AckRegistry back to helper's Postman

    case reg@RegisterClient(_) =>
      // sender() is helper's Postman
      log.info("Receive RegisterClient from {}", sender())
      server ! (reg, sender())

    case ack@AckRegistry(clientKey, serverId) =>
      log.info("AckRegistry from server {}", serverId)
      server ! (ack, sender()) // sender() is servant's Postman

    case p@ClientSegment(conn, seq, data, clientKey) =>
      // Receive from helper's Postman, send to local server to process
      server ! (p, 1)

    case (p@ClientSegment(conn, seq, data, clientKey), servant: ActorRef) =>
      log.info("Spread {} bytes segment to {}", data.size(), servant)
      servant ! p

    case s@SendToClient(clientKey, Some(data)) =>
      log.info("Send segment: conn {}, seq {}, size {}", data.conn, data.seq, data.data.size())
      server ! s

    case (s@SendToClient(clientKey, Some(data)), servant: ActorRef) =>
      log.info("Send to client {} {} bytes", clientKey, data.data.size())
      servant ! s

    case (s@ServerSegment(_, _, _), servant: ActorRef) =>
      servant ! s
  }
}