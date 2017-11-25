package me.sticnarf.agga

import akka.actor.Actor
import akka.cluster.Cluster
import akka.cluster.ClusterEvent.{MemberEvent, UnreachableMember}
import akka.event.Logging

class ClusterController extends Actor {
  val log = Logging(context.system, this)
  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = {
    cluster.unsubscribe(self)
  }

  override def receive = {
    case x: MemberEvent => log.info("MemberEvent: {}", x)
    case x: UnreachableMember => log.info("UnreachableMember {}: ", x)
  }
}
