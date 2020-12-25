import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.LeaderInfo
import com.stakkato95.raft.behavior.Candidate
import com.stakkato95.raft.behavior.Candidate.{RequestVote, VoteGranted}
import com.stakkato95.raft.behavior.Follower.AppendEntriesHeartbeat
import com.stakkato95.raft.behavior.base.BaseCommand
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

class CandidateSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Candidate" must {

    "become Leader after quorum of nodes votes for it" in {
      val follower1 = createTestProbe[BaseCommand]()
      val follower2 = createTestProbe[BaseCommand]()

      val candidate = spawn(Candidate(
        nodeId = "node-1",
        timeout = CandidateSpec.TIMEOUT,
        log = ArrayBuffer(),
        cluster = ArrayBuffer(follower1.ref, follower2.ref)
      ))

      val requestVoteMsg = RequestVote(
        candidateTerm = 1,
        candidate = candidate.ref,
        lastLogItem = None
      )
      follower1.expectMessage(requestVoteMsg)
      follower2.expectMessage(requestVoteMsg)

      candidate ! VoteGranted
      candidate ! VoteGranted

      val msg = AppendEntriesHeartbeat(LeaderInfo(
        term = 1,
        leader = candidate.ref
      ))
      follower1.expectMessage(msg)
      follower2.expectMessage(msg)
    }

    "restart election, if election timer elapses" in {

    }
  }
}

object CandidateSpec {
  private val TIMEOUT = FiniteDuration(2, TimeUnit.SECONDS)
}