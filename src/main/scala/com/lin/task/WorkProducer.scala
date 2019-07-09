package com.lin.task

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorRef

object WorkProducer {

  case object Tick

}

class WorkProducer(frontend: ActorRef) extends Actor with ActorLogging {

  import WorkProducer._
  import context.dispatcher

  def scheduler = context.system.scheduler

  def rnd = ThreadLocalRandom.current

  def nextWorkId(): String = UUID.randomUUID().toString

  var n = 0

  override def preStart(): Unit =
    scheduler.scheduleOnce(1 seconds, self, Tick)

  // override postRestart so we don't call preStart and schedule a new Tick
  override def postRestart(reason: Throwable): Unit = ()

  def receive = {
    case Tick =>
      n += 1
      log.info("Produced work: {}", n)
      val work = Work(nextWorkId(), n)
      // 向master发送 work开始请求
      frontend ! work
      context.become(waitAccepted(work), discardOld = false)

  }

  // 等待master响应
  def waitAccepted(work: Work): Actor.Receive = {
    // 完成,继续下次请求
    case  Frontend.Ok =>
      context.unbecome()
      scheduler.scheduleOnce(1 seconds, self, Tick)
    //未完成,重试之前的任务
    case Frontend.NotOk =>
      log.info("Work not accepted, retry after a while")
      scheduler.scheduleOnce(3 seconds, frontend, work)
  }

}