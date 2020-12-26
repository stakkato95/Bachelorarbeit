import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.behavior.Candidate.{RequestVote, VoteGranted}
import com.stakkato95.raft.behavior.Follower
import com.stakkato95.raft.behavior.Follower.{AppendEntriesHeartbeat, AppendEntriesNewLog}
import com.stakkato95.raft.behavior.Leader.AppendEntriesResponse
import com.stakkato95.raft.behavior.base.BaseCommand
import com.stakkato95.raft.uuid.Uuid
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

class FollowerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Follower" must {

    "become Candidate if heartbeat timer elapses" in {
      val node1 = createTestProbe[BaseCommand]()
      val node2 = createTestProbe[BaseCommand]()

      val follower = spawn(Follower(
        nodeId = FollowerSpec.NODE_ID,
        timeout = FollowerSpec.TIMEOUT,
        log = ArrayBuffer(),
        cluster = ArrayBuffer(node1.ref, node2.ref),
        stateMachineValue = ""
      ))

      val msg = RequestVote(
        candidateTerm = 1,
        candidate = follower.ref,
        lastLogItem = None
      )
      node1.expectMessage(msg)
      node2.expectMessage(msg)
    }

    "accepts a new log item from Leader with up-to-date log" in {
      val leader = createTestProbe[BaseCommand]()

      val nodeId = "node-1"
      val leaderTerm = 3
      val followerInitialLog = ArrayBuffer[LogItem](
        LogItem(1, "a"),
        LogItem(1, "b"),
        LogItem(2, "c"),
        LogItem(2, "d"),
        LogItem(leaderTerm, "e"),
      )
      val previousLogItem = LastLogItem(
        index = followerInitialLog.size - 1,
        leaderTerm = followerInitialLog.last.leaderTerm
      )
      val leaderCommit = followerInitialLog.size - 1
      val logItemUuid = Uuid.get

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(leader.ref),
        stateMachineValue = ""
      ))

      follower ! AppendEntriesNewLog(
        leaderInfo = LeaderInfo(leaderTerm, leader.ref),
        previousLogItem = Some(previousLogItem),
        newLogItem = "new",
        leaderCommit = leaderCommit,
        logItemUuid = Some(logItemUuid)
      )

      leader.expectMessage(AppendEntriesResponse(
        success = true,
        logItemUuid = Some(logItemUuid),
        nodeId = nodeId,
        replyTo = follower.ref
      ))
    }

    "report error if Follower is behind leaders log" in {
      val leader = createTestProbe[BaseCommand]()

      val nodeId = "node-1"
      val leaderTerm = 4
      val followerInitialLog = ArrayBuffer[LogItem](
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(3, "c"),
      )
      val leaderCommit = 5
      val previousLogItem = LastLogItem(
        index = leaderCommit,
        leaderTerm = leaderTerm
      )
      val logItemUuid = Uuid.get

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(leader.ref),
        stateMachineValue = ""
      ))

      follower ! AppendEntriesNewLog(
        leaderInfo = LeaderInfo(leaderTerm, leader.ref),
        previousLogItem = Some(previousLogItem),
        newLogItem = "new",
        leaderCommit = leaderCommit,
        logItemUuid = Some(logItemUuid)
      )

      leader.expectMessage(AppendEntriesResponse(
        success = false,
        logItemUuid = Some(logItemUuid),
        nodeId = nodeId,
        replyTo = follower.ref
      ))
    }

    "append new log items from an up-to-date Leader" in {
      //TODO add a request to get current state of log
    }

    "ignore Candidate with an old term" in {
      val currentLeader = createTestProbe[BaseCommand]()
      val candidate = createTestProbe[BaseCommand]()

      val nodeId = "node-1"
      val followerInitialLog = ArrayBuffer[LogItem](
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(3, "c"),
      )
      val leaderTerm = 3
      val oldLeaderTerm = 3

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(currentLeader.ref, candidate.ref),
        stateMachineValue = ""
      ))

      follower ! AppendEntriesHeartbeat(LeaderInfo(
        term = leaderTerm,
        leader = currentLeader.ref
      ))

      follower ! RequestVote(
        candidateTerm = oldLeaderTerm,
        candidate = candidate.ref,
        lastLogItem = Some(LastLogItem(index = followerInitialLog.size - 2, leaderTerm = oldLeaderTerm))
      )

      candidate.expectNoMessage()
    }

    "ignore Candidate with an outdated log" in {
      val currentLeader = createTestProbe[BaseCommand]()
      val candidate = createTestProbe[BaseCommand]()

      val nodeId = "node-1"
      val followerInitialLog = ArrayBuffer[LogItem](
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(3, "c"),
      )
      val leaderTerm = 3
      val candidateTerm = 3

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(currentLeader.ref, candidate.ref),
        stateMachineValue = ""
      ))

      follower ! AppendEntriesHeartbeat(LeaderInfo(
        term = leaderTerm,
        leader = currentLeader.ref
      ))

      follower ! RequestVote(
        candidateTerm = candidateTerm,
        candidate = candidate.ref,
        lastLogItem = Some(LastLogItem(index = followerInitialLog.size - 2, leaderTerm = candidateTerm))
      )

      candidate.expectNoMessage()
    }

    "accept Candidate with an up-to-date log and a new term" in {
      val currentLeader = createTestProbe[BaseCommand]()
      val candidate1 = createTestProbe[BaseCommand]()

      val nodeId = "node-1"
      val leaderTerm = 3
      val followerInitialLog = ArrayBuffer[LogItem](
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(leaderTerm, "c"),
      )
      val candidateTerm = 4

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(currentLeader.ref, candidate1.ref),
        stateMachineValue = ""
      ))

      follower ! AppendEntriesHeartbeat(LeaderInfo(
        term = leaderTerm,
        leader = currentLeader.ref
      ))

      follower ! RequestVote(
        candidateTerm = candidateTerm,
        candidate = candidate1.ref,
        lastLogItem = Some(LastLogItem(index = followerInitialLog.size - 1, leaderTerm = leaderTerm))
      )
      candidate1.expectMessage(VoteGranted)
    }

    "ignore one more Candidate in the same term" in {
      val currentLeader = createTestProbe[BaseCommand]()
      val candidate1 = createTestProbe[BaseCommand]()
      val candidate2 = createTestProbe[BaseCommand]()

      val nodeId = "node-1"
      val leaderTerm = 3
      val followerInitialLog = ArrayBuffer[LogItem](
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(leaderTerm, "c"),
      )
      val candidateTerm = 4

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(currentLeader.ref, candidate1.ref, candidate2.ref),
        stateMachineValue = ""
      ))

      follower ! AppendEntriesHeartbeat(LeaderInfo(
        term = leaderTerm,
        leader = currentLeader.ref
      ))

      follower ! RequestVote(
        candidateTerm = candidateTerm,
        candidate = candidate1.ref,
        lastLogItem = Some(LastLogItem(index = followerInitialLog.size - 1, leaderTerm = leaderTerm))
      )
      candidate1.expectMessage(VoteGranted)

      follower ! RequestVote(
        candidateTerm = candidateTerm,
        candidate = candidate2.ref,
        lastLogItem = Some(LastLogItem(index = followerInitialLog.size - 1, leaderTerm = leaderTerm))
      )
      candidate2.expectNoMessage()
    }

    "be brought by Leader into consistent state if Follower's log is diverged" in {
      //TODO
    }
  }
}

object FollowerSpec {

  private val NODE_ID = "node-1"

  private val TIMEOUT = FiniteDuration(2, TimeUnit.SECONDS)
}