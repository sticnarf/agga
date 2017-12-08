package me.sticnarf.agga.server.actors

import akka.actor.{Actor, ActorLogging}
import akka.io.Tcp.Received
import me.sticnarf.agga.messages.ServerSegment
import me.sticnarf.agga.server.messages.SendToClient

class Segmenter(val conn: Int, val clientKey: String) extends Actor with ActorLogging {
  val balancer = context.actorSelection("/user/balancer")

  val SEGMENT_SIZE = 3900

  var currentSeq = 0

  override def receive: Receive = {
    case r@Received(data) =>
      data.grouped(SEGMENT_SIZE).foreach { bytes =>
        val seq = currentSeq
        currentSeq += 1
        // Send the whole data to the local balancer now
        balancer ! SendToClient(clientKey,
          Some(ServerSegment(conn, seq, com.google.protobuf.ByteString.copyFrom(bytes.asByteBuffer))))
      }

    case x => log.error("Unknown message: {}", x)
  }
}