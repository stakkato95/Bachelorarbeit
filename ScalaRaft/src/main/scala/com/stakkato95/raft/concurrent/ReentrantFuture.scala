package com.stakkato95.raft.concurrent

trait ReentrantFuture[T] {

  def getWithTimeout(timeoutMillis: Long): Option[T]

  def get(): Option[T]
}