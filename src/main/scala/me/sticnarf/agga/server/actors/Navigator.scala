package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.SendToAll
import me.sticnarf.agga.messages.{Connect, ServerInfo, ServerList}
import me.sticnarf.agga.server.messages.{RequestServer, ResponseServer}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._

class Navigator extends Actor with ActorLogging {
  val cluster = Cluster(context.system)
  val mediator = DistributedPubSub(context.system).mediator
  val timeout = 500 millisecond
  val receivedServers = mutable.HashMap[Int, ArrayBuffer[ServerInfo]]()
  var counter = 1

  override def receive = {
    case c: Connect =>
      log.debug("Receive connect request. Key: {}, Known servers: {}", c.key, c.knownServers)

      val seq = counter
      counter += 1

      receivedServers.put(seq, ArrayBuffer())
      mediator ! SendToAll("/user/addressProvider", RequestServer(seq, c.key))

      import scala.concurrent.ExecutionContext.Implicits.global
      context.system.scheduler.scheduleOnce(timeout, self, Timeout(seq, sender()))

    case s: ResponseServer =>
      for (list <- receivedServers.get(s.seq);
           serverInfo <- s.info)
        list.append(serverInfo)

    case Timeout(seq, remoteSender) =>
      log.debug("Waiting finished")
      receivedServers.remove(seq).foreach(remoteSender ! ServerList(_))
  }

  case class Timeout(seq: Int, remoteSender: ActorRef)

}
