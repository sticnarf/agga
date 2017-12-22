package me.sticnarf.agga.server.actors.main

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import me.sticnarf.agga.messages.{Ack, ClientSegment, Connect}
import me.sticnarf.agga.server.actors.cluster.Postman
import me.sticnarf.agga.server.messages.{AckRegistry, RegisterClient, SendToClient}
import me.sticnarf.agga.server.{AggaConfig, ServerMain}

import scala.collection.mutable

object Server {
  private val remotes = mutable.HashMap[String, ActorRef]()
  private val postmen = mutable.HashMap[String, ActorRef]()
  private val servants = mutable.HashMap[String, ActorRef]()

  def props: Props = Props(classOf[Server], remotes, postmen, servants)
}

class Server(val remotes: mutable.HashMap[String, ActorRef],
             val postmen: mutable.HashMap[String, ActorRef],
             val servants: mutable.HashMap[String, ActorRef]) extends Actor with ActorLogging {
  private val postman = ServerMain.clusterSystem.actorSelection("/user/postman")

  override def receive: Receive = {
    case c@Connect(key) =>
      log.info("Connection from {}", key)
      remotes.put(key, sender())
      postman ! c

    case (reg@RegisterClient(key), redirector: ActorRef) =>
      // Accept a helper
      // Not thread safe?
      if (AggaConfig.acceptedKeys.contains(key)) {
        // Create a servant if this is the first helper
        val servant = servants.getOrElseUpdate(key, context.actorOf(Props(classOf[ClientServant], key)))
        servant ! (reg, redirector) // redirector is helper's Postman
      }

    case (ack@AckRegistry(clientKey, serverId), servant: ActorRef) =>
      postmen.put(clientKey, servant) // servant is servant's Postman
      remotes.get(clientKey).foreach(_ ! Ack(AggaConfig.serverId)) // Send Ack to client

    case p@ClientSegment(conn, seq, data, clientKey) =>
      // Receive segment from client
      postman ! (p, postmen(clientKey))

    case s@SendToClient(clientKey, Some(data)) =>
      remotes(clientKey) ! data

    case (p@ClientSegment(conn, seq, data, clientKey), 1) =>
      // Segment to be processed
      servants(clientKey) ! p
  }

}
