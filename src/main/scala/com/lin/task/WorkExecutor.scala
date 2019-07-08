package com.lin.task

import akka.actor.Actor

class WorkExecutor(workProcess: WorkProcess) extends Actor {

  def receive = {
    case n: Int =>
      val n2 = n * n
      val result = s"$n * $n = $n2"
      sender() ! Worker.WorkComplete(result)
    case workSource: WorkSource =>
      val result = workProcess.process(workSource)
      sender() ! Worker.WorkComplete(result)

  }

}