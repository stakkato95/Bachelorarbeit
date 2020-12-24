import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}
import com.stakkato95.raft.behavior.Candidate.RequestVote
import com.stakkato95.raft.behavior.Follower.AppendEntriesNewLog
import com.stakkato95.raft.behavior.Leader.AppendEntriesResponse
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
        nodeId = FollowerSpec.NODE_ID,
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
      val logItemUuid = UUID.randomUUID().toString

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(leader.ref)
      ))

      follower ! AppendEntriesNewLog(
        leaderInfo = LeaderInfo(leaderTerm, leader.ref),
        previousLogItem = previousLogItem,
        newLogItem = "new",
        leaderCommit = leaderCommit,
        logItemUuid = logItemUuid
      )

      leader.expectMessage(AppendEntriesResponse(
        success = true,
        logItemUuid = logItemUuid,
        nodeId = nodeId
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
      val logItemUuid = UUID.randomUUID().toString

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(leader.ref)
      ))

      follower ! AppendEntriesNewLog(
        leaderInfo = LeaderInfo(leaderTerm, leader.ref),
        previousLogItem = previousLogItem,
        newLogItem = "new",
        leaderCommit = leaderCommit,
        logItemUuid = logItemUuid
      )

      leader.expectMessage(AppendEntriesResponse(
        success = false,
        logItemUuid = logItemUuid,
        nodeId = nodeId
      ))
    }

    //TODO SEND INFO ABOUT THE LATEST LEADER TO THIS "NON LEADER"
    "???decline a new log item from Leader with outdated log" in {

    }
  }
}

object FollowerSpec {

  private val NODE_ID = "node-1"

  private val TIMEOUT = FiniteDuration(2, TimeUnit.SECONDS)
}