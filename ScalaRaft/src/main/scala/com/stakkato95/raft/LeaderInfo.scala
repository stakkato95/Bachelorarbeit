package com.stakkato95.raft

import akka.actor.typed.ActorRef
import com.stakkato95.raft.behavior.Command

case class LeaderInfo(term: Int, address: ActorRef[Command])
