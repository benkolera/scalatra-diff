package com.benkolera.scalatradiff

import org.mortbay.jetty.Server
import org.mortbay.jetty.servlet.{Context,ServletHolder}

object DiffApp { 

  def main(args: Array[String]) = {
    val port = 
      args match {
        case Array(port) => Some(port.toInt)
        case _ => None
      }

    val server = new Server(port.getOrElse(8080))
    val root = new Context(server,"/",Context.SESSIONS)
    root.addServlet(new ServletHolder(new DiffServlet()), "/*")
    server.start()
  }

}
