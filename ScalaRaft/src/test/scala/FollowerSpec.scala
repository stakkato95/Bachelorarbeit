import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.behavior.Candidate.RequestVote
import com.stakkato95.raft.behavior.{BaseCommand, Follower, Leader}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

class FollowerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Follower" must {

    "become Candidate if heartbeat timer elapses" in {
      val node1 = createTestProbe[BaseCommand]()
      val node2 = createTestProbe[BaseCommand]()

      val follower = spawn(Follower(
        nodeId = "node-1",
        timeout = FollowerSpec.TIMEOUT,
        log = ArrayBuffer(),
        cluster = ArrayBuffer(node1.ref, node2.ref)
      ))

      val msg = RequestVote(
        candidateTerm = Leader.INITIAL_TERM,
        candidate = follower.ref,
        lastLogItem = None
      )
      node1.expectMessage(msg)
      node2.expectMessage(msg)
    }
  }
}

object FollowerSpec {
  val TIMEOUT = FiniteDuration(2, TimeUnit.SECONDS)
}