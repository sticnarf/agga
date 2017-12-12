package me.sticnarf.agga.server

import java.net.InetSocketAddress
import java.util.UUID

import com.typesafe.config.ConfigFactory

object AggaConfig {
  private val config = ConfigFactory.load().getConfig("agga")
  private val tcpRedirConfig = config.getConfig("tcp-redir")

  val tcpAddr = new InetSocketAddress(tcpRedirConfig.getString("hostname"), tcpRedirConfig.getInt("port"))
  val serverId = UUID.randomUUID().toString

  import scala.collection.JavaConverters._

  val acceptedKeys = Set(config.getStringList("accepted-keys").asScala: _*)
}
