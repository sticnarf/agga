package me.sticnarf.agga.server

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.routing.RoundRobinPool
import me.sticnarf.agga.server.actors.{AddressProvider, Navigator, Redirector, SocksServer}

object AggaServer extends App {
  val system = ActorSystem("agga")
  val id = UUID.randomUUID()
  val clusterController = system.actorOf(Props[ClusterController], "clusterController")

  val address = Cluster(system).selfAddress
  val addressProvider = system.actorOf(
    Props(classOf[AddressProvider], id.toString, address.toString),
    "addressProvider"
  )

  val socksServer = system.actorOf(RoundRobinPool(5).props(Props[SocksServer]), "socksServer")

  val redirector = system.actorOf(RoundRobinPool(5).props(Props[Redirector]), "redirector")

  val navigators = system.actorOf(RoundRobinPool(5).props(Props[Navigator]), "navigator")
  ClusterClientReceptionist(system).registerService(navigators)
}
