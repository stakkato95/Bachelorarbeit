import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.LeaderInfo
import com.stakkato95.raft.behavior.Candidate.{RequestVote, VoteGranted}
import com.stakkato95.raft.behavior.Follower
import com.stakkato95.raft.behavior.Follower.{AppendEntriesHeartbeat, AppendEntriesNewLog}
import com.stakkato95.raft.behavior.Leader.AppendEntriesResponse
import com.stakkato95.raft.behavior.base.BaseCommand
import com.stakkato95.raft.log.{LogItem, PreviousLogItem}
import com.stakkato95.raft.uuid.Uuid
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.language.postfixOps

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
        stateMachineValue = "",
        lastApplied = None
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
      val previousLogItem = PreviousLogItem(
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
        stateMachineValue = "",
        lastApplied = None
      ))

      follower ! AppendEntriesNewLog(
        leaderInfo = LeaderInfo(leaderTerm, leader.ref),
        previousLogItem = Some(previousLogItem),
        newLogItem = LogItem(leaderTerm = leaderTerm, value = "new"),
        leaderCommit = Some(leaderCommit),
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
      val previousLogItem = PreviousLogItem(
        index = leaderCommit,
        leaderTerm = leaderTerm
      )
      val logItemUuid = Uuid.get

      val follower = spawn(Follower(
        nodeId = nodeId,
        timeout = FollowerSpec.TIMEOUT,
        log = followerInitialLog,
        cluster = ArrayBuffer(leader.ref),
        stateMachineValue = "abc",
        lastApplied = Some(2)
      ))

      follower ! AppendEntriesNewLog(
        leaderInfo = LeaderInfo(leaderTerm, leader.ref),
        previousLogItem = Some(previousLogItem),
        newLogItem = LogItem(leaderTerm = leaderTerm, value = "new"),
        leaderCommit = Some(leaderCommit),
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
        stateMachineValue = "",
        lastApplied = None
      ))

      follower ! AppendEntriesHeartbeat(
        leaderInfo = LeaderInfo(
          term = leaderTerm,
          leader = currentLeader.ref
        ),
        leaderCommit = None
      )

      follower ! RequestVote(
        candidateTerm = oldLeaderTerm,
        candidate = candidate.ref,
        lastLogItem = Some(PreviousLogItem(index = followerInitialLog.size - 2, leaderTerm = oldLeaderTerm))
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
        stateMachineValue = "",
        lastApplied = None
      ))

      follower ! AppendEntriesHeartbeat(
        leaderInfo = LeaderInfo(
          term = leaderTerm,
          leader = currentLeader.ref
        ),
        leaderCommit = None
      )

      follower ! RequestVote(
        candidateTerm = candidateTerm,
        candidate = candidate.ref,
        lastLogItem = Some(PreviousLogItem(index = followerInitialLog.size - 2, leaderTerm = candidateTerm))
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
        stateMachineValue = "",
        lastApplied = None
      ))

      follower ! AppendEntriesHeartbeat(
        leaderInfo = LeaderInfo(
          term = leaderTerm,
          leader = currentLeader.ref
        ),
        leaderCommit = None
      )

      follower ! RequestVote(
        candidateTerm = candidateTerm,
        candidate = candidate1.ref,
        lastLogItem = Some(PreviousLogItem(index = followerInitialLog.size - 1, leaderTerm = leaderTerm))
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
        stateMachineValue = "",
        lastApplied = None
      ))

      follower ! AppendEntriesHeartbeat(
        leaderInfo = LeaderInfo(
          term = leaderTerm,
          leader = currentLeader.ref
        ),
        leaderCommit = None
      )

      follower ! RequestVote(
        candidateTerm = candidateTerm,
        candidate = candidate1.ref,
        lastLogItem = Some(PreviousLogItem(index = followerInitialLog.size - 1, leaderTerm = leaderTerm))
      )
      candidate1.expectMessage(VoteGranted)

      follower ! RequestVote(
        candidateTerm = candidateTerm,
        candidate = candidate2.ref,
        lastLogItem = Some(PreviousLogItem(index = followerInitialLog.size - 1, leaderTerm = leaderTerm))
      )
      candidate2.expectNoMessage()
    }

    "be brought by Leader into consistent state if Follower's log is diverged" in {
      val leader = createTestProbe[BaseCommand]()
      val follower2 = createTestProbe[BaseCommand]()

      val leaderTerm = 4
      val leaderInfo = LeaderInfo(leaderTerm, leader.ref)
      val leaderLog = ArrayBuffer(
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(3, "c")
      )

      val log = ArrayBuffer(
        LogItem(1, "a"),
        LogItem(1, "h"),
        LogItem(2, "i")
      )
      val follower = spawn(Follower(
        nodeId = FollowerSpec.NODE_ID,
        timeout = 60 seconds,
        log = log,
        cluster = ArrayBuffer(leader.ref, follower2.ref),
        stateMachineValue = "ahi",
        lastApplied = Some(2)
      ))

      //establish leadership
      var leaderCommit = 2
      follower ! AppendEntriesHeartbeat(leaderInfo = leaderInfo, leaderCommit = Some(leaderCommit))

      // Client sends request to Leader with new item "d"
      val newValue = "d"
      leaderLog += LogItem(4, newValue)


      ////////////////////////////////////////////////////////////
      // Leader start to replicate. It should save new item to its own log, replicate it to follower2 and follower
      ////////////////////////////////////////////////////////////
      val appendNewItem = AppendEntriesNewLog(
        leaderInfo = leaderInfo,
        previousLogItem = Some(PreviousLogItem(index = 2, leaderTerm = 3)),
        newLogItem = LogItem(leaderTerm = leaderTerm, value = newValue),
        leaderCommit = Some(leaderCommit),
        logItemUuid = None //for test purposes uuid is irrelevant
      )
      follower ! appendNewItem
      leaderCommit = 3


      ////////////////////////////////////////////////////////////
      // Follower should remove last item from log and send AppendEntriesResponse(success = false)
      ////////////////////////////////////////////////////////////
      val followerUnsuccessful = AppendEntriesResponse(
        success = false,
        logItemUuid = None,
        nodeId = FollowerSpec.NODE_ID,
        replyTo = follower.ref
      )
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerUnsuccessful)
      log should ===(ArrayBuffer(LogItem(1, "a"), LogItem(1, "h")))


      // Leader sends retry with "LogItem(2, "b")" as previous and "c" as new log item
      val retry1 = AppendEntriesNewLog(
        leaderInfo = leaderInfo,
        previousLogItem = Some(PreviousLogItem(index = 1, leaderTerm = 2)),
        newLogItem = LogItem(3, "c"),
        leaderCommit = Some(leaderCommit),
        logItemUuid = None //for test purposes uuid is irrelevant
      )
      follower ! retry1


      ////////////////////////////////////////////////////////////
      // Follower should remove last item from log and send AppendEntriesResponse(success = false)
      ////////////////////////////////////////////////////////////
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerUnsuccessful)
      log should ===(ArrayBuffer(LogItem(1, "a")))


      // Leader sends retry with "LogItem(1, "a")" as previous and "b" as new log item
      val retry2 = AppendEntriesNewLog(
        leaderInfo = leaderInfo,
        previousLogItem = Some(PreviousLogItem(index = 0, leaderTerm = 1)),
        newLogItem = LogItem(2, "b"),
        leaderCommit = Some(leaderCommit),
        logItemUuid = None //for test purposes uuid is irrelevant
      )
      follower ! retry2


      ////////////////////////////////////////////////////////////
      // Follower should send AppendEntriesResponse(success = true) and append LogItem(2, "b") to its log
      ////////////////////////////////////////////////////////////
      val followerSuccessful = AppendEntriesResponse(
        success = true,
        logItemUuid = None,
        nodeId = FollowerSpec.NODE_ID,
        replyTo = follower.ref
      )
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerSuccessful)
      log should ===(ArrayBuffer(LogItem(1, "a"), LogItem(2, "b")))


      // Leader sends retry with "LogItem(2, "b")" as previous and "c" as new log item
      follower ! retry1


      ////////////////////////////////////////////////////////////
      // Follower should send AppendEntriesResponse(success = false) and append LogItem(3, "c") to its log
      ////////////////////////////////////////////////////////////
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerSuccessful)
      log should ===(ArrayBuffer(LogItem(1, "a"), LogItem(2, "b"), LogItem(3, "c")))


      // Leader sends appendNewItem with "LogItem(3, "c")" as previous and "d" as new log item
      follower ! appendNewItem


      ////////////////////////////////////////////////////////////
      // Follower log should be consistent
      ////////////////////////////////////////////////////////////
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerSuccessful)
      log should ===(leaderLog)

      //TODO check that "currentStateMachineValue" inside Follower is correct
    }

    "be brought by Leader into consistent state if Follower's log is empty" in {
      val leader = createTestProbe[BaseCommand]()
      val follower2 = createTestProbe[BaseCommand]()

      val leaderTerm = 4
      val leaderInfo = LeaderInfo(leaderTerm, leader.ref)
      val leaderLog = ArrayBuffer(
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(3, "c")
      )

      val log = ArrayBuffer[LogItem]()
      val follower = spawn(Follower(
        nodeId = FollowerSpec.NODE_ID,
        timeout = 60 seconds,
        log = log,
        cluster = ArrayBuffer(leader.ref, follower2.ref),
        stateMachineValue = "",
        lastApplied = None
      ))

      //establish leadership
      var leaderCommit = 2
      follower ! AppendEntriesHeartbeat(leaderInfo = leaderInfo, leaderCommit = Some(2))

      // Client sends request to Leader with new item "d"
      val newValue = "d"
      leaderLog += LogItem(4, newValue)


      ////////////////////////////////////////////////////////////
      // Leader start to replicate. It should save new item to its own log, replicate it to follower2 and follower
      ////////////////////////////////////////////////////////////

      val appendNewItem = AppendEntriesNewLog(
        leaderInfo = leaderInfo,
        previousLogItem = Some(PreviousLogItem(index = 2, leaderTerm = 3)),
        newLogItem = LogItem(leaderTerm = leaderTerm, value = newValue),
        leaderCommit = Some(leaderCommit),
        logItemUuid = None //for test purposes uuid is irrelevant
      )
      follower ! appendNewItem
      leaderCommit = 3


      ////////////////////////////////////////////////////////////
      // Follower should remove last item from log and send AppendEntriesResponse(success = false)
      ////////////////////////////////////////////////////////////
      val followerUnsuccessful = AppendEntriesResponse(
        success = false,
        logItemUuid = None,
        nodeId = FollowerSpec.NODE_ID,
        replyTo = follower.ref
      )
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerUnsuccessful)
      log.size should ===(0)


      // Leader sends retry with "LogItem(2, "b")" as previous and "c" as new log item
      val retry1 = AppendEntriesNewLog(
        leaderInfo = leaderInfo,
        previousLogItem = Some(PreviousLogItem(index = 1, leaderTerm = 2)),
        newLogItem = LogItem(3, "c"),
        leaderCommit = Some(leaderCommit),
        logItemUuid = None //for test purposes uuid is irrelevant
      )
      follower ! retry1


      ////////////////////////////////////////////////////////////
      // Follower should remove last item from log and send AppendEntriesResponse(success = false)
      ////////////////////////////////////////////////////////////
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerUnsuccessful)
      log.size should ===(0)


      // Leader sends retry with "LogItem(1, "a")" as previous and "b" as new log item
      val retry2 = retry1.copy(
        previousLogItem = Some(PreviousLogItem(index = 0, leaderTerm = 1)),
        newLogItem = LogItem(2, "b")
      )
      follower ! retry2


      ////////////////////////////////////////////////////////////
      // Follower should remove last item from log and send AppendEntriesResponse(success = false)
      ////////////////////////////////////////////////////////////
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerUnsuccessful)
      log.size should ===(0)


      // Leader sends retry with "None" as previous and "a" as new log item
      follower ! retry1.copy(previousLogItem = None, newLogItem = LogItem(1, "a"))


      ////////////////////////////////////////////////////////////
      // Follower should send AppendEntriesResponse(success = true) and append LogItem(1, "a") to its log
      ////////////////////////////////////////////////////////////
      val followerSuccessful = AppendEntriesResponse(
        success = true,
        logItemUuid = None,
        nodeId = FollowerSpec.NODE_ID,
        replyTo = follower.ref
      )
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerSuccessful)
      log should ===(ArrayBuffer(LogItem(1, "a")))


      // Leader sends retry with "LogItem(1, "a")" as previous and "b" as new log item
      follower ! retry2


      ////////////////////////////////////////////////////////////
      // Follower should send AppendEntriesResponse(success = true) and append LogItem(2, "b") to its log
      ////////////////////////////////////////////////////////////
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerSuccessful)
      log should ===(ArrayBuffer(LogItem(1, "a"), LogItem(2, "b")))


      // Leader sends retry with "LogItem(2, "b")" as previous and "c" as new log item
      follower ! retry1


      ////////////////////////////////////////////////////////////
      // Follower should send AppendEntriesResponse(success = true) and append LogItem(3, "c") to its log
      ////////////////////////////////////////////////////////////
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerSuccessful)
      log should ===(ArrayBuffer(LogItem(1, "a"), LogItem(2, "b"), LogItem(3, "c")))


      // Leader sends retry with "LogItem(3, "c")" as previous and "appendNewItem" as new log item
      follower ! appendNewItem


      ////////////////////////////////////////////////////////////
      // Follower should send AppendEntriesResponse(success = true) and append LogItem(4, "d") to its log
      // Follower log should be consistent
      ////////////////////////////////////////////////////////////
      leader.expectMessage(FollowerSpec.EXPECT_MSG_TIMEOUT, followerSuccessful)
      log should ===(leaderLog)

      //TODO check that "currentStateMachineValue" inside Follower is correct
    }
  }
}

object FollowerSpec {

  private val NODE_ID = "node-1"

  private val TIMEOUT = 2 seconds

  // for test purposes
  private val EXPECT_MSG_TIMEOUT = 60 seconds
}