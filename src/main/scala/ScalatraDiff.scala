package com.benkolera.scalatradiff

import org.scalatra._
import org.scalatra.fileupload._
import org.apache.commons.fileupload.FileItem
import ScalaDiff._

class DiffServlet extends ScalatraServlet with FileUploadSupport {

  before {
    contentType = "text/html"
  }

  get("/") {
    <form method="post" enctype="multipart/form-data">
      <p>Old File: <input type="file" name="oldFile" /></p>
      <p>New File: <input type="file" name="newFile" /></p>
      <input type="submit" />
    </form>
  }

  post("/") {
    val oldFile = fileParams("oldFile").getString
    val newFile = fileParams("newFile").getString

    
    <div>
    {
      diff(oldFile,newFile).map{
        case Insert(t) => mkDiffSpan(t=t,colour="green")
        case Delete(t) => mkDiffSpan(t=t,colour="red",strike=true)
        case Equal(t) => mkDiffSpan(t=t)
      }
    }
    </div>
  }

  def mkDiffSpan(
    t: String, colour: String = "black", 
    strike: Boolean = false
  ) = {
    val style = "white-space:pre;color:"+colour + (
      if (strike) ";text-decoration: line-through" else ""
    )
    <span style={style}>{t}</span>
  }
 
}
