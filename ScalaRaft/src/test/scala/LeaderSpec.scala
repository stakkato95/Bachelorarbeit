import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.{PreviousLogItem, LeaderInfo, LogItem}
import com.stakkato95.raft.behavior.Follower.{AppendEntriesHeartbeat, AppendEntriesNewLog}
import com.stakkato95.raft.behavior.Leader
import com.stakkato95.raft.behavior.Leader.{AppendEntriesResponse, ClientRequest, ClientResponse}
import com.stakkato95.raft.behavior.base.BaseCommand
import com.stakkato95.raft.uuid.UuidProvider
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.FiniteDuration

class LeaderSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "Leader" must {

    "reply with ClientResponse when new value is replicated to quorum" in {
      val client = createTestProbe[ClientResponse]()
      val follower1 = createTestProbe[BaseCommand]()
      val follower2 = createTestProbe[BaseCommand]()
      val follower3 = createTestProbe[BaseCommand]()
      val follower4 = createTestProbe[BaseCommand]()

      val log = ArrayBuffer(
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(3, "c"),
      )

      val leaderTerm = 4
      val leader = spawn(Leader(
        nodeId = "leader",
        timeout = FiniteDuration(4, TimeUnit.SECONDS),
        log = log,
        cluster = ArrayBuffer(follower1.ref, follower2.ref, follower3.ref, follower4.ref),
        leaderTerm = leaderTerm,
        uuidProvider = LeaderSpec.UUID_PROVIDER,
        stateMachineValue = "abc"
      ))

      leader ! ClientRequest(value = "d", client.ref)

      leader ! AppendEntriesResponse(success = true, Some(LeaderSpec.UUID), "follower-1", follower1.ref)
      leader ! AppendEntriesResponse(success = true, Some(LeaderSpec.UUID), "follower-2", follower2.ref)

      client.expectMessage(ClientResponse("abcd"))
    }

    "not reply with ClientResponse when new value is not replicated to quorum" in {
      val client = createTestProbe[ClientResponse]()
      val follower1 = createTestProbe[BaseCommand]()
      val follower2 = createTestProbe[BaseCommand]()
      val follower3 = createTestProbe[BaseCommand]()
      val follower4 = createTestProbe[BaseCommand]()

      val log = ArrayBuffer(
        LogItem(1, "a"),
        LogItem(2, "b"),
        LogItem(3, "c"),
      )

      val leaderTerm = 4
      val leader = spawn(Leader(
        nodeId = "leader",
        timeout = FiniteDuration(4, TimeUnit.SECONDS),
        log = log,
        cluster = ArrayBuffer(follower1.ref, follower2.ref, follower3.ref, follower4.ref),
        leaderTerm = leaderTerm,
        uuidProvider = LeaderSpec.UUID_PROVIDER,
        stateMachineValue = "abc"
      ))

      leader ! ClientRequest(value = "d", client.ref)

      leader ! AppendEntriesResponse(success = true, Some(LeaderSpec.UUID), "follower-1", follower1.ref)

      client.expectNoMessage()
    }

    "bring Follower with diverged log into consistent state" in {
      val follower1 = createTestProbe[BaseCommand]()
      val follower2 = createTestProbe[BaseCommand]()
      val client = createTestProbe[ClientResponse]()

      //only the very first item is same as in Leader's log => log diverges
      val onlyFirstItemIsSame = LogItem(1, "a")
      val followerLog = ArrayBuffer(
        onlyFirstItemIsSame,
        LogItem(1, "h"),
        LogItem(2, "i"),
      )

      val leaderTerm = 4
      val log = ArrayBuffer(
        onlyFirstItemIsSame,
        LogItem(2, "b"),
        LogItem(3, "c"),
      )
      val stateMachineValue = "abc"
      val leader = spawn(Leader(
        nodeId = "leader",
        timeout = FiniteDuration(4, TimeUnit.SECONDS),
        log = log,
        cluster = ArrayBuffer(follower1.ref, follower2.ref),
        leaderTerm = leaderTerm,
        uuidProvider = LeaderSpec.UUID_PROVIDER,
        stateMachineValue = stateMachineValue
      ))

      //followers expect heartbeat after leader has established its leadership
      follower1.expectMessageType[AppendEntriesHeartbeat]
      follower2.expectMessageType[AppendEntriesHeartbeat]

      val newLogItem = "d"
      leader ! ClientRequest(newLogItem, client.ref)

      ///////////////////////////////////////////////////////////////////////////////////////////
      // Leader starts replicating log to followers
      ///////////////////////////////////////////////////////////////////////////////////////////
      val leaderInfo = LeaderInfo(term = leaderTerm, leader = leader.ref)
      val appendAntriesMsg = AppendEntriesNewLog(
        leaderInfo = leaderInfo,
        previousLogItem = Some(PreviousLogItem(index = 2, leaderTerm = 3)),
        newLogItem = newLogItem,
        leaderCommit = 2,
        logItemUuid = Some(LeaderSpec.UUID)
      )
      //leader sends AppendEntriesNewLog to all followers
      follower1.expectMessage(appendAntriesMsg)
      follower2.expectMessage(appendAntriesMsg)

      ///////////////////////////////////////////////////////////////////////////////////////////
      // First Follower responds with success=true, i.e. successfully replicated
      // It means that Leader can now apply item to its state machine an make it committed (increment commitIndex)
      ///////////////////////////////////////////////////////////////////////////////////////////
      // simulate follower1 sending AppendEntriesResponse to Leader
      leader ! AppendEntriesResponse(
        success = true,
        logItemUuid = Some(LeaderSpec.UUID),
        nodeId = "follower-1",
        replyTo = follower1.ref
      )
      //after that follower1 shouldn't send other messages
      follower1.expectNoMessage()
      //client should receive current state of replicated state machine,
      //since data is already replicated to 2 nodes (leader & follower1)
      client.expectMessage(ClientResponse(stateMachineValue + newLogItem))

      ///////////////////////////////////////////////////////////////////////////////////////////
      // Second Follower responds with success=false, i.e. last log item on this Follower
      // is not equal to the PRE-penultimate item on Leader
      ///////////////////////////////////////////////////////////////////////////////////////////
      // PRE-penultimate item "c" on Leader and the last item "i" on follower2 are not equal
      // prove it by comparing
      log(log.size - 2) should !==(followerLog.last)
      val failedAppendEntriesMsg = AppendEntriesResponse(
        success = false,
        logItemUuid = Some(LeaderSpec.UUID),
        nodeId = "follower-2",
        replyTo = follower2.ref
      )
      // simulate follower2 sending AppendEntriesResponse(success = false) to Leader
      leader ! failedAppendEntriesMsg

      // diverged log is found => leader must repair follower2 log
      val leaderCommitWhenRetrying = log.size - 1
      val retryFirst = AppendEntriesNewLog(
        leaderInfo = leaderInfo,
        previousLogItem = Some(PreviousLogItem(index = 1, leaderTerm = 2)),
        newLogItem = "c",
        leaderCommit = leaderCommitWhenRetrying,
        logItemUuid = None //uuid is not important, since success=false is received from one particular node
      )

      // follower2 should receive AppendEntriesNewLog to repair its LogItem at index 2 => LogItem(3, "i")
      follower2.expectMessage(retryFirst)
      followerLog.remove(followerLog.size - 1) //Follower removes diverged item

      ///////////////////////////////////////////////////////////////////////////////////////////
      // Second Follower AGAIN responds with success=false, i.e. last log item on this Follower
      // is STILL not equal to the PRE-penultimate item on Leader
      ///////////////////////////////////////////////////////////////////////////////////////////
      // PRE-penultimate item on Leader (LogItem(2, "b")) is not equal to LogItem(2, "h") on follower2
      log(log.size - 3) should !==(followerLog.last)
      leader ! failedAppendEntriesMsg

      val retrySecond = AppendEntriesNewLog(
        leaderInfo = leaderInfo,
        previousLogItem = Some(PreviousLogItem(index = 0, leaderTerm = 1)),
        newLogItem = "b",
        leaderCommit = leaderCommitWhenRetrying,
        logItemUuid = None //uuid is not important, since success=false is received from one particular node
      )
      // follower2 should receive AppendEntriesNewLog to repair its LogItem at index 1 => LogItem(2, "h")
      follower2.expectMessage(retrySecond)
      // "LogItem(2, "b")" is successfully replicated to follower2
      // prove it by comparing logs
      followerLog.remove(followerLog.size - 1) //Follower removes diverged item
      log(log.size - 4) should ===(followerLog.last)
      followerLog += log(log.size - 3) //append second item from Leader to the log of the follower
      log.take(followerLog.size) should ===(followerLog)


      ///////////////////////////////////////////////////////////////////////////////////////////
      // Second Follower FINALLY responds with success=TRUE, i.e. last log item on this Follower
      // (now it is also the first item, because Follower removes diverged items)
      // is EQUAL to the FIRST on item Leader
      ///////////////////////////////////////////////////////////////////////////////////////////
      val follower2success = AppendEntriesResponse(
        success = true,
        logItemUuid = None,
        nodeId = "follower-2",
        replyTo = follower2.ref
      )
      leader ! follower2success
      follower2.expectMessage(retryFirst)
      followerLog += log(log.size - 2) //append second item from Leader to the log of the follower
      log.take(followerLog.size) should ===(followerLog)


      ///////////////////////////////////////////////////////////////////////////////////////////
      // Second Follower responds with success=true, i.e. last log item on this Follower
      // is equal to the penultimate item on Leader
      ///////////////////////////////////////////////////////////////////////////////////////////
      leader ! follower2success
      follower2.expectMessage(appendAntriesMsg.copy(leaderCommit = 3, logItemUuid = None))
      followerLog += log(log.size - 1) //append second item from Leader to the log of the follower
      log should ===(followerLog)
    }
  }
}

object LeaderSpec {

  private val UUID = "uuid"

  private val UUID_PROVIDER = new UuidProvider {
    override def get: String = UUID
  }
}