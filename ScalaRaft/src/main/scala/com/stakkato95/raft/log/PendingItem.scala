package com.stakkato95.raft.log

import akka.actor.typed.ActorRef
import com.stakkato95.raft.RaftClient.ClientResponse

/**
 *
 * @param logItem
 * @param votes Number of nodes to which logItem was replicated
 * @param replyTo
 */
case class PendingItem(logItem: LogItem, var votes: Int, replyTo: ActorRef[ClientResponse])
