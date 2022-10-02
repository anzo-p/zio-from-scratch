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
  final case class Running(callbacks: List[Exit[E, A] => Any]) extends FiberState
  final case class Done(result: Exit[E, A]) extends FiberState

  type Erased       = ZIO[Any, Any]
  type ErasedFold   = Fold[Any, Any, Any, Any]
  type Continuation = Any => Erased

  var currentExecutor: ExecutionContext = initExecutor

  var currentZIO: Erased = erase(zio)

  val stack = new mutable.Stack[Continuation]()

  val state: AtomicReference[FiberState] =
    new AtomicReference(Running(List.empty))

  override def interrupt: ZIO[Nothing, Unit] = ???

  override def join: ZIO[E, A] =
    ZIO
      .async[Exit[E, A]] { callback =>
        await(callback)
      }
      .flatMap(ZIO.done)

  def await(callback: Exit[E, A] => Any): Unit = {
    var loopState = true
    while (loopState) {
      state.get() match {

        case oldState @ Running(callbacks) =>
          val newState = Running(callback :: callbacks)
          loopState = !state.compareAndSet(oldState, newState)
          println(s"[FiberContext] - await/running - prepended $callback to $callbacks")

        case Done(result) =>
          println(s"[FiberContext] - await/done - ready with $result - callback with $callback")
          callback(result)
          loopState = false
      }
    }
  }

  def complete(result: Exit[E, A]): Unit = {
    var loopState = true
    while (loopState) {
      state.get() match {

        case oldState @ Running(callbacks) =>
          if (state.compareAndSet(oldState, Done(result))) {
            loopState = false
            callbacks.foreach { cb =>
              println(s"[FiberContext] - complete - ready with $result - callback with $cb")
              cb(result)
            }
          }

        case Done(_) =>
          throw new Exception("Fiber completed more than once")
      }
    }
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
            println(s"[FiberContext] - found errorHandler $foldType")

          case _ =>
        }
      }
    }

    errorHandler
  }

  def run(): Unit = {
    var loopStack = true

    def continue(value: Any): Unit =
      if (stack.isEmpty) {
        loopStack = false
        complete(Exit.succeed(value.asInstanceOf[A]))
        println(s"[FiberContext] - complete with ${Exit.succeed(value.asInstanceOf[A])}")
      }
      else {
        val continuation = stack.pop()
        currentZIO = continuation(value)
        println(s"[FiberContext] - continue with $currentZIO")
      }

    while (loopStack) {
      println(s"[FiberContext] - run current zio - $currentZIO")
      currentZIO match {

        case ZIO.Async(register) =>
          loopStack = false
          if (stack.isEmpty) {
            register { a =>
              complete(Exit.succeed(a.asInstanceOf[A]))
            }
          }
          else {
            register { a =>
              currentZIO = ZIO.succeedNow(a)
              run()
            }
          }

        case ZIO.Fail(e) =>
          val errorHandler = findNextErrorHandler()
          if (errorHandler eq null) {
            complete(Exit.fail(e().asInstanceOf[E]))
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
  }

  currentExecutor.execute { () =>
    run()
  }
}
