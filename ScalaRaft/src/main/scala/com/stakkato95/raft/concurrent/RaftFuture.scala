package com.stakkato95.raft.concurrent

class RaftFuture[T](lock: AnyRef) extends ReentrantFuture[T] {

  private var result: Option[T] = None

  def set(value: T): Unit = {
    result = Some(value)
  }

  override def get(): T = {
    //TODO blocks thread forever, if ReentrantPromise.success was called once
    //and will never be called in the future. Implemented with awareness of this risk

    lock.synchronized {
      lock.wait()
      result.get
    }
  }
}