package me.sticnarf.agga.server

import akka.actor.{ActorSystem, Address, Props}
import akka.remote.BoundAddressesExtension
import com.typesafe.config.ConfigFactory
import me.sticnarf.agga.server.actors.cluster.{AddressProvider, ClusterController, Postman}
import me.sticnarf.agga.server.actors.main.{Receptionist, Server}

object ServerMain {
  val clusterSystem = ActorSystem("cluster")
  val aggaSystem = ActorSystem("agga", ConfigFactory.load().getConfig("agga"))
  val address: Address = BoundAddressesExtension(aggaSystem).boundAddresses("akka.tcp").head

  def main(args: Array[String]): Unit = {
    clusterSystem.actorOf(Props[ClusterController], "clusterController")
    clusterSystem.actorOf(Props[AddressProvider], "addressProvider")

    // TODO: Postman is not thread-safe yet
    // val redirector = system.actorOf(RoundRobinPool(5).props(Postman.props(AggaConfig.serverId)), "postman")
    clusterSystem.actorOf(Props[Postman], "postman")

    aggaSystem.actorOf(Server.props, "server")
    aggaSystem.actorOf(Props[Receptionist], "receptionist")
  }
}
