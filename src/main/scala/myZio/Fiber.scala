package myZio

import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable
import scala.concurrent.ExecutionContext

sealed trait Fiber[+A] {

  def interrupt: ZIO[Unit]

  def join: ZIO[A]
}

private class FiberContext[A](zio: ZIO[A], initExecutor: ExecutionContext) extends Fiber[A] {

  sealed trait FiberState
  final case class Running(callbacks: List[A => Any]) extends FiberState
  final case class Done(result: A) extends FiberState

  type Erased       = ZIO[Any]
  type Continuation = Any => Erased

  var currentExecutor: ExecutionContext = initExecutor

  var currentZIO: Erased = erase(zio)

  var loop = true

  var state: AtomicReference[FiberState] =
    new AtomicReference(Running(List.empty))

  val stack = new mutable.Stack[Continuation]()

  override def interrupt: ZIO[Unit] = ???

  override def join: ZIO[A] =
    ZIO.async { callback =>
      await(callback)
    }

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

  def continue(value: Any): Unit =
    if (stack.isEmpty) {
      stop()
      complete(value.asInstanceOf[A])
    }
    else {
      val continuation = stack.pop()
      currentZIO = continuation(value)
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

  def erase[B](zio: ZIO[B]): Erased =
    zio

  def resume(): Unit = {
    loop = true
    run()
  }

  def stop(): Unit =
    loop = false

  def run(): Unit =
    while (loop) {
      currentZIO match {

        case ZIO.Async(register) =>
          if (stack.isEmpty) {
            stop()
            register { a =>
              complete(a.asInstanceOf[A])
            }
          }
          else {
            stop()
            register { a =>
              currentZIO = ZIO.succeedNow(a)
              resume()
            }
          }

        case ZIO.Effect(thunk) =>
          continue(thunk())

        case ZIO.FlatMap(zio, continuation) =>
          stack.push(continuation.asInstanceOf[Continuation])
          currentZIO = zio

        case ZIO.Fork(zio) =>
          val fiber = new FiberContext(zio, currentExecutor)
          continue(fiber)

        case ZIO.Shift(executor) =>
          currentExecutor = executor
          continue(())

        case ZIO.Succeed(value) =>
          continue(value)
      }
    }

  currentExecutor.execute { () =>
    run()
  }
}
