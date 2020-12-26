package com.stakkato95.raft

import akka.actor.typed.ActorRef
import com.stakkato95.raft.behavior.Leader.ClientResponse

/**
 *
 * @param logItem
 * @param votes Number of nodes to which logItem was replicated
 * @param replyTo
 */
case class PendingItem(logItem: LogItem, var votes: Int, replyTo: ActorRef[ClientResponse])
