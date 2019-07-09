package com.lin.task

import akka.actor.{Actor, ActorRef}
import com.lin.task.WorkProducer.Tick

import scala.collection.immutable.Queue
import scala.concurrent.duration._


/**
  * @author chonglin
  * @param frontend
  */
case class DefaultWorkProducer(frontend: ActorRef) extends WorkProducer(frontend: ActorRef) {
  private var workSourceQueue: Queue[WorkSource] = Queue.empty[WorkSource]

  import context.dispatcher
  override def preStart(): Unit =()
  override def receive = {
    case Tick =>
      val (workSource, rest) = workSourceQueue.dequeue
      workSourceQueue = rest
      val work = Work(nextWorkId(), workSource)
      frontend ! work
      context.become(waitAccepted1(work), discardOld = false)
    case workSource: WorkSource =>
      log.info("workSource {}",workSource.toString)
      workSourceQueue = workSourceQueue enqueue workSource

  }

  // 等待master响应
  def waitAccepted1(work: Work): Actor.Receive = {
    // 完成,继续下次请求
    case Frontend.Ok =>
      context.unbecome()
      scheduler.scheduleOnce(50 microseconds, self, Tick)
    //未完成,重试之前的任务
    case Frontend.NotOk =>
      log.info("Work not accepted, retry after a while")
      scheduler.scheduleOnce(500 microseconds, frontend, work)
  }
}
