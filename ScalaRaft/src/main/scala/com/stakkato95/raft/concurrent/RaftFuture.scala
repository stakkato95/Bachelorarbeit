package com.stakkato95.raft.concurrent

class RaftFuture[T](lock: AnyRef) extends ReentrantFuture[T] {

  private var result: Option[T] = None
  private var isValueSet = false

  def set(value: T): Unit = {
    isValueSet = true
    result = Some(value)
  }

  def getWithTimeout(timeoutMillis: Long): Option[T] = {
    //TODO blocks thread forever, if ReentrantPromise.success was called once
    //and will never be called in the future. Implemented with awareness of this risk

    lock.synchronized {
      lock.wait(timeoutMillis)
      if (isValueSet) {
        isValueSet = false
        Some(result.get)
      } else {
        None
      }
    }
  }

  def get(): Option[T] = {
    getWithTimeout(0)
  }
}