package com.lin.task

import akka.actor.ActorRef
import com.lin.task.WorkProducer.Tick

/**
  *chonglin
  * @param workSource
  * @param frontend
  */
case class DefaultWorkProducer(workSource:WorkSource, frontend: ActorRef) extends WorkProducer(frontend: ActorRef) {
  override def receive = {
    case Tick =>
      log.info("Produced work: {}", workSource.id)
      val work = Work(nextWorkId(), workSource)
      frontend ! work
      context.become(waitAccepted(work), discardOld = false)

  }
}
