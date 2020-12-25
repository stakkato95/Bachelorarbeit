import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.stakkato95.raft.LogItem
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
      val uuid = "uuid"
      val leader = spawn(Leader(
        nodeId = "leader",
        timeout = FiniteDuration(4, TimeUnit.SECONDS),
        log = log,
        cluster = ArrayBuffer(follower1.ref, follower2.ref, follower3.ref, follower4.ref),
        leaderTerm = leaderTerm,
        uuidProvider = new UuidProvider {
          override def get: String = uuid
        },
        stateMachineValue = "abc"
      ))

      leader ! ClientRequest(value = "d", client.ref)

      leader ! AppendEntriesResponse(success = true, uuid, "follower-1")
      leader ! AppendEntriesResponse(success = true, uuid, "follower-2")

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
      val uuid = "uuid"
      val leader = spawn(Leader(
        nodeId = "leader",
        timeout = FiniteDuration(4, TimeUnit.SECONDS),
        log = log,
        cluster = ArrayBuffer(follower1.ref, follower2.ref, follower3.ref, follower4.ref),
        leaderTerm = leaderTerm,
        uuidProvider = new UuidProvider {
          override def get: String = uuid
        },
        stateMachineValue = "abc"
      ))

      leader ! ClientRequest(value = "d", client.ref)

      leader ! AppendEntriesResponse(success = true, uuid, "follower-1")

      client.expectNoMessage()
    }
  }
}
