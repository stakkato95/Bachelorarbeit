package com.stakkato95.raft.behavior

object Leader {

  trait Command extends BaseCommand

  case class AppendEntriesResponse(success: Boolean, logItemUuid: String, nodeId: String) extends Command

  val INITIAL_TERM = 0
}


class Leader {

}
