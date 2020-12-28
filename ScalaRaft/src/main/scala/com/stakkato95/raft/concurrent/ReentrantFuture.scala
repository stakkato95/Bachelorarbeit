package com.stakkato95.raft.concurrent

trait ReentrantFuture {

  def get[U](): U
}