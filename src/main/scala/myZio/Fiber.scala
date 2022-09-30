package myZio

import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.ExecutionContext

sealed trait Fiber[+A] {

  def join: ZIO[A]

  def start(): Unit
}

class FiberImpl[A](zio: ZIO[A]) extends Fiber[A] {

  sealed trait FiberState
  final case class Running(callbacks: List[A => Any]) extends FiberState
  final case class Done(result: A) extends FiberState

  var state: AtomicReference[FiberState] =
    new AtomicReference(Running(List.empty))

  def await(callback: A => Any): Unit = {
    var loop = true
    while (loop) {
      val oldState = state.get()
      oldState match {

        case Running(callbacks) =>
          val newState = Running(callback :: callbacks)
          loop = !state.compareAndSet(oldState, newState)

        case Done(result) =>
          callback(result)
          loop = false
      }
    }
  }

  def complete(result: A): Unit = {
    var loop = true
    while (loop) {
      val oldState = state.get()
      oldState match {

        case Running(callbacks) =>
          if (state.compareAndSet(oldState, Done(result))) {
            loop = false
            callbacks.foreach { cb =>
              cb(result)
            }
          }

        case Done(_) =>
          throw new Exception("Fiber completed more than once")
      }
    }
  }

  override def join: ZIO[A] =
    ZIO.async { callback =>
      await(callback)
    }

  override def start(): Unit =
    ExecutionContext.global.execute { () =>
      zio.run(complete)
    }
}
