package com.benkolera.scalatradiff 

import name.fraser.neil.plaintext.diff_match_patch
import diff_match_patch.Operation
import scala.collection.JavaConversions._

object ScalaDiff {

  val diffObj = new diff_match_patch()

  sealed trait Diff
  case class Insert(text: String) extends Diff
  case class Delete(text: String) extends Diff
  case class Equal(text: String) extends Diff
  
  def diff(a:String, b: String):List[Diff] = {
    diffObj.diff_main(a,b).map{
      d => d.operation match {
        case Operation.DELETE => Delete(d.text)
        case Operation.INSERT => Insert(d.text)
        case Operation.EQUAL => Equal(d.text)
      }
    }.toList
  }

}
