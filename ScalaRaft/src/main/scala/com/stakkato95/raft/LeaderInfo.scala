package com.stakkato95.raft

import akka.actor.typed.ActorRef
import com.stakkato95.raft.behavior.Leader

case class LeaderInfo(term: Int, leader: ActorRef[Leader.Command])
