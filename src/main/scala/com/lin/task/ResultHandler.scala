package com.lin.task

object ResultHandler {
  def handler(resultHandler: ResultHandler, result: WorkResult): Unit = {
    resultHandler.handle(result)
  }
}

abstract class ResultHandler {
  def handle(result: WorkResult)
}


case class DefaultResultHandler() extends ResultHandler {
  override def handle(result: WorkResult): Unit = {
    result.result match {
      case WorkRes(str)=>
        println("Consumed result: {}", str)
      case _ =>
        println(result.result)
    }
  }
}
