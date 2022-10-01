package myZio

import myZio.ZIO.Fold

import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable
import scala.concurrent.ExecutionContext

sealed trait Fiber[+E, +A] {

  def interrupt: ZIO[Nothing, Unit]

  def join: ZIO[E, A]
}

private class FiberContext[E, A](zio: ZIO[E, A], initExecutor: ExecutionContext) extends Fiber[E, A] {

  sealed trait FiberState
  final case class Running(callbacks: List[Either[E, A] => Any]) extends FiberState
  final case class Done(result: Either[E, A]) extends FiberState

  type Erased       = ZIO[Any, Any]
  type ErasedFold   = Fold[Any, Any, Any, Any]
  type Continuation = Any => Erased

  var currentExecutor: ExecutionContext = initExecutor

  var currentZIO: Erased = erase(zio)

  var loopStack = true

  val stack = new mutable.Stack[Continuation]()

  var state: AtomicReference[FiberState] =
    new AtomicReference(Running(List.empty))

  override def interrupt: ZIO[Nothing, Unit] = ???

  override def join: ZIO[E, A] =
    ZIO
      .async[Either[E, A]] { callback =>
        await(callback)
      }
      .flatMap(ZIO.fromEither)

  def await(callback: Either[E, A] => Any): Unit = {
    var loopState = true
    while (loopState) {
      val oldState = state.get()
      oldState match {

        case Running(callbacks) =>
          val newState = Running(callback :: callbacks)
          loopState = !state.compareAndSet(oldState, newState)

        case Done(result) =>
          callback(result)
          loopState = false
      }
    }
  }

  def complete(result: Either[E, A]): Unit = {
    var loopStates = true
    while (loopStates) {
      val oldState = state.get()
      oldState match {

        case Running(callbacks) =>
          if (state.compareAndSet(oldState, Done(result))) {
            loopStates = false
            callbacks.foreach { cb =>
              cb(result)
            }
          }

        case Done(_) =>
          throw new Exception("Fiber completed more than once")
      }
    }
  }

  def continue(value: Any): Unit =
    if (stack.isEmpty) {
      stop()
      complete(Right(value.asInstanceOf[A]))
    }
    else {
      val continuation = stack.pop()
      currentZIO = continuation(value)
    }

  def erase[E1 >: E, B](zio: ZIO[E1, B]): Erased =
    zio

  def findNextErrorHandler(): ErasedFold = {
    var loopStack                = true
    var errorHandler: ErasedFold = null
    while (loopStack) {
      if (stack.isEmpty) {
        loopStack = false
      }
      else {
        stack.pop() match {
          case foldType: ErasedFold =>
            errorHandler = foldType
            loopStack    = false

          case _ =>
        }
      }
    }

    errorHandler
  }

  def resume(): Unit = {
    loopStack = true
    run()
  }

  def run(): Unit =
    while (loopStack) {
      currentZIO match {

        case ZIO.Async(register) =>
          if (stack.isEmpty) {
            stop()
            register { a =>
              complete(Right(a.asInstanceOf[A]))
            }
          }
          else {
            stop()
            register { a =>
              currentZIO = ZIO.succeedNow(a)
              resume()
            }
          }

        case ZIO.Fail(e) =>
          val errorHandler = findNextErrorHandler()
          if (errorHandler eq null) {
            complete(Left(e().asInstanceOf[E]))
          }
          else {
            currentZIO = errorHandler.failure(e())
          }

        case ZIO.FlatMap(zio, continuation) =>
          stack.push(continuation.asInstanceOf[Continuation])
          currentZIO = zio

        case fold @ ZIO.Fold(zio, _, _) =>
          stack.push(fold)
          currentZIO = zio

        case ZIO.Fork(zio) =>
          val fiber = new FiberContext(zio, currentExecutor)
          continue(fiber)

        case ZIO.Shift(executor) =>
          currentExecutor = executor
          continue(())

        case ZIO.Succeed(thunk) =>
          continue(thunk())

        case ZIO.SucceedNow(value) =>
          continue(value)
      }
    }

  def stop(): Unit =
    loopStack = false

  currentExecutor.execute { () =>
    run()
  }
}
