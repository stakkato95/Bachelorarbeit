import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.{LastLogItem, LeaderInfo, LogItem}
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

      //follower log could be this
      //only the very first item is same as in Leader's log
      val onlyFirstItemIsSame = LogItem(1, "a")
      val followerLog = ArrayBuffer(
        onlyFirstItemIsSame,
        LogItem(2, "h"),
        LogItem(3, "i"),
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

      follower1.expectMessageType[AppendEntriesHeartbeat]
      follower2.expectMessageType[AppendEntriesHeartbeat]

      val newLogItem = "d"
      leader ! ClientRequest(newLogItem, client.ref)
      follower1.awaitAssert({}, FiniteDuration(2, TimeUnit.SECONDS))

      //"- 2" for "previousIndexWhenReplicating" because at this point "d" is already appended to log
      val previousIndexWhenReplicating = log.size - 2
      //"- 2" for "leaderCommitWhenReplicating" because "d" is not yet committed to the log,
      //it is not yet replicated to the quorum of nodes
      val leaderCommitWhenReplicating = log.size - 2
      val appendEntriesMsg = AppendEntriesNewLog(
        leaderInfo = LeaderInfo(
          term = leaderTerm,
          leader = leader.ref
        ),
        previousLogItem = Some(LastLogItem(
          index = previousIndexWhenReplicating,
          leaderTerm = log(previousIndexWhenReplicating).leaderTerm
        )),
        newLogItem = newLogItem,
        leaderCommit = leaderCommitWhenReplicating,
        logItemUuid = Some(LeaderSpec.UUID)
      )
      follower1.expectMessage(appendEntriesMsg)
      follower2.expectMessage(appendEntriesMsg)

      leader ! AppendEntriesResponse(
        success = true,
        logItemUuid = Some(LeaderSpec.UUID),
        nodeId = "follower-1",
        replyTo = follower1.ref
      )
      follower1.expectNoMessage()
      client.expectMessage(ClientResponse(stateMachineValue + newLogItem))

      val failedAppendEntriesMsg = AppendEntriesResponse(
        success = false,
        logItemUuid = Some(LeaderSpec.UUID),
        nodeId = "follower-2",
        replyTo = follower2.ref
      )
      leader ! failedAppendEntriesMsg

      //log is passed to leader by reference, so it growths also in this test
      val previousIndexFirstRetry = log.size - 3
      val leaderCommitWhenRetrying = log.size - 1
      val leaderInfoWhenRetrying = LeaderInfo(term = leaderTerm, leader = leader.ref)
      val retryFirst = AppendEntriesNewLog(
        leaderInfo = leaderInfoWhenRetrying,
        previousLogItem = Some(LastLogItem(
          index = previousIndexFirstRetry,
          leaderTerm = log(previousIndexFirstRetry).leaderTerm
        )),
        newLogItem = log(previousIndexFirstRetry + 1).value,
        leaderCommit = leaderCommitWhenRetrying,
        logItemUuid = None //uuid is not important, since success=false is received from one particular node
      )
      follower2.expectMessage(retryFirst)

      leader ! failedAppendEntriesMsg

      val previousIndexSecondRetry = log.size - 4
      val retrySecond = AppendEntriesNewLog(
        leaderInfo = leaderInfoWhenRetrying,
        previousLogItem = Some(LastLogItem(
          index = previousIndexSecondRetry,
          leaderTerm = log(previousIndexSecondRetry).leaderTerm
        )),
        newLogItem = log(previousIndexSecondRetry + 1).value,
        leaderCommit = leaderCommitWhenRetrying,
        logItemUuid = None //uuid is not important, since success=false is received from one particular node
      )
      follower2.expectMessage(retrySecond)

//      leader ! AppendEntriesResponse(
//        success = true,
//        logItemUuid = None,
//        nodeId = "follower-2",
//        replyTo = follower2.ref
//      )
    }
  }
}

object LeaderSpec {

  private val UUID = "uuid"

  private val UUID_PROVIDER = new UuidProvider {
    override def get: String = UUID
  }
}