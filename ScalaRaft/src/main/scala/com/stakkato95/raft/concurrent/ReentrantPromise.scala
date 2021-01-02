package com.stakkato95.raft.concurrent

class ReentrantPromise[T] {

  private val lock = AnyRef
  private val f: RaftFuture[T] = new RaftFuture[T](lock)

  def success(value: T): Unit = {
    lock.synchronized {
      f.set(value)
      lock.notify()
    }
  }

  def future: ReentrantFuture[T] = {
    f
  }
}
