package me.sticnarf.agga.server

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.routing.RoundRobinPool
import me.sticnarf.agga.server.actors._

object AggaServer extends App {
  val system = ActorSystem("agga")
  val clusterController = system.actorOf(Props[ClusterController], "clusterController")

  val address = Cluster(system).selfAddress
  val addressProvider = system.actorOf(
    Props(classOf[AddressProvider], AggaConfig.serverId, address.toString),
    "addressProvider"
  )

  val aggregator = system.actorOf(Props[Aggregator], "aggregator")
  val balancer = system.actorOf(RoundRobinPool(5).props(Balancer.props), "balancer")

  // TODO: Postman is not thread-safe yet
  // val redirector = system.actorOf(RoundRobinPool(5).props(Postman.props(AggaConfig.serverId)), "postman")
  val redirector = system.actorOf(Postman.props(AggaConfig.serverId), "postman")

  val navigators = system.actorOf(RoundRobinPool(5).props(Props[Navigator]), "navigator")
  ClusterClientReceptionist(system).registerService(navigators)
}
