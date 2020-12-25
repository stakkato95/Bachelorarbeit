import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
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
        timeout = FiniteDuration(4, TimeUnit.SECONDS),
        log = ArrayBuffer(),
        cluster = ArrayBuffer(follower1.ref, follower2.ref)
      ))

      val requestVoteMsg = RequestVote(
        candidateTerm = 1,
        candidate = candidate.ref,
        lastLogItem = None
      )
      follower1.expectMessage(CandidateSpec.EXPECT_MESSAGE_TIMEOUT, requestVoteMsg)
      follower2.expectMessage(CandidateSpec.EXPECT_MESSAGE_TIMEOUT, requestVoteMsg)

      candidate ! VoteGranted
      candidate ! VoteGranted

      val msg = AppendEntriesHeartbeat(LeaderInfo(
        term = 1,
        leader = candidate.ref
      ))
      follower1.expectMessage(msg)
      follower2.expectMessage(msg)
    }

    "stay Candidate if less than quorum of nodes votes for it" in {
      val follower1 = createTestProbe[BaseCommand]()
      val follower2 = createTestProbe[BaseCommand]()
      val follower3 = createTestProbe[BaseCommand]()
      val follower4 = createTestProbe[BaseCommand]()

      val candidate = spawn(Candidate(
        nodeId = "node-1",
        timeout = FiniteDuration(4, TimeUnit.SECONDS),
        log = ArrayBuffer(),
        cluster = ArrayBuffer(follower1.ref, follower2.ref, follower3.ref, follower4.ref)
      ))

      val requestVoteMsg = RequestVote(
        candidateTerm = 1,
        candidate = candidate.ref,
        lastLogItem = None
      )
      follower1.expectMessage(CandidateSpec.EXPECT_MESSAGE_TIMEOUT, requestVoteMsg)
      follower2.expectMessage(CandidateSpec.EXPECT_MESSAGE_TIMEOUT, requestVoteMsg)
      follower3.expectMessage(CandidateSpec.EXPECT_MESSAGE_TIMEOUT, requestVoteMsg)
      follower4.expectMessage(CandidateSpec.EXPECT_MESSAGE_TIMEOUT, requestVoteMsg)

      candidate ! VoteGranted

      follower1.expectNoMessage()
      follower2.expectNoMessage()
      follower3.expectNoMessage()
      follower4.expectNoMessage()
    }

    "restart election, if election timer elapses" in {
      val follower1 = createTestProbe[BaseCommand]()

      val timeoutTime = 2
      val followerTimeout = FiniteDuration(timeoutTime + 1, TimeUnit.SECONDS)
      val candidate = spawn(Candidate(
        nodeId = "node-1",
        timeout = FiniteDuration(timeoutTime, TimeUnit.SECONDS),
        log = ArrayBuffer(),
        cluster = ArrayBuffer(follower1.ref)
      ))

      val reqVoteTermOne = RequestVote(
        candidateTerm = 1,
        candidate = candidate.ref,
        lastLogItem = None
      )
      follower1.expectMessage(followerTimeout, reqVoteTermOne)

      val reqVoteTermTwo = RequestVote(
        candidateTerm = 2,
        candidate = candidate.ref,
        lastLogItem = None
      )
      follower1.expectMessage(followerTimeout, reqVoteTermTwo)
    }

    "become follower, when receives AppendEntries from a new Leader" in {
//      val leader = createTestProbe[BaseCommand]()
//      val leaderTerm = 2
//
//      val candidate = spawn(Candidate(
//        nodeId = "node-1",
//        timeout = FiniteDuration(3, TimeUnit.SECONDS),
//        log = ArrayBuffer(),
//        cluster = ArrayBuffer(leader.ref)
//      ))
//
//      candidate ! AppendEntriesHeartbeat(LeaderInfo(term = leaderTerm, leader.ref))
    }

    "bec follower, when receives AppendEntries from a new Leader" in {

    }
  }
}

object CandidateSpec {
  private val EXPECT_MESSAGE_TIMEOUT = FiniteDuration(2, TimeUnit.SECONDS)
}