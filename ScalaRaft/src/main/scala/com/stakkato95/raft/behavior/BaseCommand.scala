package com.stakkato95.raft.behavior

import akka.actor.typed.ActorRef

trait BaseCommand

case class NodesDiscovered(nodes: List[ActorRef[BaseCommand]]) extends BaseCommand