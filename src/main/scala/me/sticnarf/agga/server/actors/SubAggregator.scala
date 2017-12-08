package me.sticnarf.agga.server.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.Tcp.Connected
import me.sticnarf.agga.messages.ClientSegment

import scala.collection.mutable

class SubAggregator(val conn: Int, val clientKey: String) extends Actor with ActorLogging {
  val tcpAddr = new InetSocketAddress("localhost", 1080) // Just for debug
  val tcpClient = context.actorOf(Props(classOf[TcpClient], tcpAddr, conn, clientKey))

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
