package com.lin.task

import scala.concurrent.duration._
import akka.actor.Actor
import akka.pattern._
import akka.util.Timeout
import akka.cluster.singleton.{ClusterSingletonProxySettings, ClusterSingletonProxy}

/**
  * @author chonglin
  */
object Frontend {
  case object Ok
  case object NotOk
}

/**
  * @author chonglin
  *         master节点的代理节点
  */
class Frontend extends Actor {
  import Frontend._
  import context.dispatcher
  val masterProxy = context.actorOf(
    ClusterSingletonProxy.props(
      settings = ClusterSingletonProxySettings(context.system).withRole("backend"),
      singletonManagerPath = "/user/master"
    ),
    name = "masterProxy")

  def receive = {
    case work =>
      // 收到work请求
      implicit val timeout = Timeout(5 microseconds)
      // 向master节点发送work开始请求
      (masterProxy ? work) map {
        // 完成返回结果
        case Master.Ack(_) => Ok
      } recover { case _ => NotOk } pipeTo sender()

  }

}