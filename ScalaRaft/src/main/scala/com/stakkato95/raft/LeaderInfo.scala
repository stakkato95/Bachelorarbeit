package com.stakkato95.raft

import akka.actor.typed.ActorRef
import com.stakkato95.raft.behavior.{BaseCommand, Leader}

case class LeaderInfo(term: Int, address: ActorRef[Leader.Command])
