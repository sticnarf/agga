package me.sticnarf.agga

import akka.actor.{Actor, ActorSystem, Props}
import akka.cluster.client.ClusterClientReceptionist
import akka.routing.BalancingPool
import me.sticnarf.agga.messages.{Bye, Greeting}

class GreetActor extends Actor {
  override def receive = {
    case Greeting(name) =>
      println(s"$name greets!")
      sender() ! Greeting("Agga")
    case Bye(name) =>
      println(s"Bye, $name!")
      sender() ! Bye("Agga")
  }
}

object Agga extends App {
  val system = ActorSystem("agga")
  //  val greetActor = system.actorOf(Props[GreetActor], name = "greet")
  val clusterController = system.actorOf(Props[ClusterController], "clusterController")

  val workers = system.actorOf(BalancingPool(5).props(Props[GreetActor]), "workers")
  ClusterClientReceptionist(system).registerService(workers)
}
