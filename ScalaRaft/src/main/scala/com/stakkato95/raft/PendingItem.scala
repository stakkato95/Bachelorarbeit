package com.stakkato95.raft

import akka.actor.typed.ActorRef
import com.stakkato95.raft.behavior.Leader.ClientResponse

case class PendingItem(logItem: LogItem, var votes: Int, replyTo: ActorRef[ClientResponse])
