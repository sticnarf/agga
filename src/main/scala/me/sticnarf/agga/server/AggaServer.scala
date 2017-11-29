package me.sticnarf.agga.server

import akka.actor.{ActorSystem, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.routing.RoundRobinPool
import me.sticnarf.agga.server.actors.Navigator

object AggaServer extends App {
  val system = ActorSystem("agga")
  val clusterController = system.actorOf(Props[ClusterController], "clusterController")

  val navigators = system.actorOf(RoundRobinPool(5).props(Props[Navigator]), "navigator")
  ClusterClientReceptionist(system).registerService(navigators)
}
