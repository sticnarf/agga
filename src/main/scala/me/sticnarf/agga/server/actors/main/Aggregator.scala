package me.sticnarf.agga.server.actors.main

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp.Connected
import me.sticnarf.agga.messages.ClientSegment
import me.sticnarf.agga.server.AggaConfig

import scala.collection.mutable

class Aggregator(val conn: Int, val clientKey: String, val servant: ActorRef) extends Actor with ActorLogging {
  val tcpClient = context.actorOf(Props(classOf[TcpClient], AggaConfig.tcpAddr, conn, clientKey, servant))

  implicit val ord = PacketOrdering
  val queue = mutable.PriorityQueue[ClientSegment]()

  var currentSeq = 0

  var connected = false

  override def receive: Receive = {
    case c@Connected(_, _) =>
      log.info("Conn {} connected", conn)
      connected = true
      pourAll

    case p@ClientSegment(_, seq, data, _) =>
      log.info("Sub-aggregator {} {} received {} bytes", conn, self, data.size())
      if (connected && seq == currentSeq) {
        tcpClient ! akka.util.ByteString(data.asReadOnlyByteBuffer())
        currentSeq += 1
        pourAll
      } else {
        queue.enqueue(p)
      }

    case "close" =>
      tcpClient ! "close"
      context stop self
  }

  def pourAll = {
    while (queue.nonEmpty && queue.head.seq == currentSeq) {
      val p = queue.dequeue()
      tcpClient ! akka.util.ByteString(p.data.asReadOnlyByteBuffer())
      currentSeq += 1
    }
  }

  object PacketOrdering extends Ordering[ClientSegment] {
    override def compare(x: ClientSegment, y: ClientSegment): Int = {
      (y.seq - x.seq) compare 0
    }
  }

}
