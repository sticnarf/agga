package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import me.sticnarf.agga.messages.ClientSegment

import scala.collection.mutable

class Aggregator extends Actor with ActorLogging {
  val subAggregators = mutable.HashMap[Int, ActorRef]()

  override def receive: Receive = {
    case p@ClientSegment(conn, _, _, clientKey) =>
      val aggregator = subAggregators.getOrElseUpdate(conn, context.actorOf(Props(classOf[SubAggregator], conn, clientKey)))
      log.info("Redirect to sub-aggregator {}", conn)
      aggregator forward p
  }
}
