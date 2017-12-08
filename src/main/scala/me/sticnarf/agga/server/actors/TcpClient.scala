package me.sticnarf.agga.server.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString

class TcpClient(val remote: InetSocketAddress, val conn: Int, val clientKey: String) extends Actor with ActorLogging {
  val segmenter = context.actorOf(Props(classOf[Segmenter], conn, clientKey))
  val aggregator = context.parent

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  override def receive: Receive = {
    case CommandFailed(_: Connect) =>
      segmenter ! "connect failed"
      context stop self

    case c@Connected(remoteAddr, localAddr) =>
      log.info("Connected to {}, send to aggregator {}", remoteAddr, aggregator)
      aggregator ! c
      val connection = sender()
      connection ! Register(self)
      context become {
        case data: ByteString =>
          log.info("Send to TCP client {} bytes", data.length)
          connection ! Write(data)
        case CommandFailed(w: Write) =>
          // O/S buffer was full
          segmenter ! "write failed"
        case r@Received(data) =>
          log.info("TcpClient {} received {} bytes", conn, data.length)
          segmenter ! r
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          segmenter ! "connection closed"
          context stop self
      }
  }
}
