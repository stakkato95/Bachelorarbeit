package services

import akka.actor.typed.ActorSystem
import com.stakkato95.raft.behavior.Client
import com.stakkato95.raft.behavior.Client.{ClientRequest, ClientStart}
import com.stakkato95.raft.concurrent.ReentrantPromise
import javax.inject.Inject
import models.{ClusterItem, ClusterState, Person}

class ClusterService @Inject()() {

  private val promise = new ReentrantPromise[AnyRef]()
  private val future = promise.future
  private val actorSystem = ActorSystem(Client(promise), "client")
  actorSystem ! ClientStart

  def showPerson(person: Person): Unit = {
    println(s"success $person")
  }

  def addItemToCluster(item: ClusterItem): ClusterState = {
    actorSystem ! ClientRequest(item.value, actorSystem.ref)

    future.get[String]() match {
      case Some(clusterState) =>
        ClusterState(state = clusterState)
      case None =>
        ClusterState(state = "no state")
    }
  }
}
