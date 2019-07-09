package com.lin.task

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import akka.actor.{Actor, ActorIdentity, ActorPath, ActorRef, ActorSystem, AddressFromURIString, Identify, PoisonPill, Props, RootActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientReceptionist, ClusterClientSettings}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import akka.japi.Util.immutableSeq
import akka.persistence.journal.leveldb.SharedLeveldbStore
import akka.persistence.journal.leveldb.SharedLeveldbJournal
import akka.util.Timeout
import akka.pattern.ask

object BootStrap {

  def main(args: Array[String]): Unit = {
    val workSource = WorkSource("1", "work1", new Array[Byte](10))

    if (args.isEmpty) {
      startBackend(2551, "backend")
      Thread.sleep(5000)
      startBackend(2552, "backend")
      startWorker(0, WorkProcess())
      Thread.sleep(5000)
      startFrontend(0, classOf[DefaultWorkProducer], classOf[WorkResultConsumer], DefaultResultHandler())
    } else {
      val port = args(0).toInt
      if (2000 <= port && port <= 2999)
        startBackend(port, "backend")
      else if (3000 <= port && port <= 3999)
        startFrontend(port, classOf[DefaultWorkProducer], classOf[WorkResultConsumer], DefaultResultHandler())
      else
        startWorker(port, WorkProcess())
    }

  }

  def workTimeout = 10.seconds

  def startBackend(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]").
      withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)).
      withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)

    startupSharedJournal(system, startStore = (port == 2551), path =
      ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

    system.actorOf(
      ClusterSingletonManager.props(
        Master.props(workTimeout),
        PoisonPill,
        ClusterSingletonManagerSettings(system).withRole(role)
      ),
      "master")

  }

  def startFrontend[T <: WorkProducer, S <: WorkResultConsumer](port: Int, producer: Class[T], consumer: Class[S], resultHandler: ResultHandler): Unit = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)
    val frontend = system.actorOf(Props[Frontend], "frontend")
    val produce1 = system.actorOf(Props(producer, frontend), "producer")
    system.actorOf(Props(consumer, resultHandler), "consumer")
    produce(produce1)
  }

  def startWorker(port: Int, processor: WorkProcess): Unit = {
    // load worker.conf
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load("worker"))
    val system = ActorSystem("WorkerSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) ⇒ RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clusterClient = system.actorOf(
      ClusterClient.props(
        ClusterClientSettings(system)
          .withInitialContacts(initialContacts)),
      "clusterClient")

    system.actorOf(Worker.props(clusterClient, Props(classOf[WorkExecutor], processor)), "worker")
  }

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore)
      system.actorOf(Props[SharedLeveldbStore], "store")
    // register the shared journal
    import system.dispatcher
    implicit val timeout = Timeout(15 seconds)
    val f = (system.actorSelection(path) ? Identify(None))
    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.terminate()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.terminate()
    }
  }

  def produce(producer: ActorRef): Unit = {
    var n = 1
    while (true){
      producer! Tick(WorkSource(n.toString,n.toString,n))
      n += 1
//      Thread.sleep(1)
    }

  }
}
