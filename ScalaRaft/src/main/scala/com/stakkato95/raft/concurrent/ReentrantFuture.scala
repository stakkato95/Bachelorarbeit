package com.stakkato95.raft.concurrent

trait ReentrantFuture {

  def getWithTimeout[U](timeoutMillis: Long): Option[U]

  def get[U](): Option[U]
}