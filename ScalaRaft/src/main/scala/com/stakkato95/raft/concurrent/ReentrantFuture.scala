package com.stakkato95.raft.concurrent

trait ReentrantFuture[T] {

  def get(): T
}