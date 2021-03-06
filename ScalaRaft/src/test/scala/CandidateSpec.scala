import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.LeaderInfo
import com.stakkato95.raft.behavior.Candidate
import com.stakkato95.raft.behavior.Candidate.{RequestVote, VoteGranted}
import com.stakkato95.raft.behavior.Follower.AppendEntriesHeartbeat
import com.stakkato95.raft.behavior.base.BaseCommand
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

class CandidateSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Candidate" must {

    "become Leader after quorum of nodes votes for it" in {
      val follower1 = createTestProbe[BaseCommand]()
      val follower2 = createTestProbe[BaseCommand]()

      val candidate = spawn(Candidate(
        nodeId = "node-1",
        timeout = 4 seconds,
        log = ArrayBuffer(),
        cluster = ArrayBuffer(follower1.ref, follower2.ref),
        stateMachineValue = ""
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

      val msg = AppendEntriesHeartbeat(
        leaderInfo = LeaderInfo(
          term = 1,
          leader = candidate.ref
        ),
        leaderCommit = None
      )
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
        timeout = 4 seconds,
        log = ArrayBuffer(),
        cluster = ArrayBuffer(follower1.ref, follower2.ref, follower3.ref, follower4.ref),
        stateMachineValue = ""
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
      val followerTimeout = (timeoutTime + 1) seconds
      val candidate = spawn(Candidate(
        nodeId = "node-1",
        timeout = timeoutTime seconds,
        log = ArrayBuffer(),
        cluster = ArrayBuffer(follower1.ref),
        stateMachineValue = ""
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
      //TODO add debug event to follower. When follower starts send "FollowerStarted(nodeId)"
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
  }
}

object CandidateSpec {
  private val EXPECT_MESSAGE_TIMEOUT = 2 seconds
}