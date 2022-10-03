package myZio

import myZio.ZIO.Fold
import myZio.types.{Cause, Exit}

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import scala.collection.mutable
import scala.concurrent.ExecutionContext

sealed trait Fiber[+E, +A] {

  def interrupt: ZIO[Any, Nothing, Unit]

  def join: ZIO[Any, E, A]
}

private class FiberContext[E, A](zio: ZIO[Any, E, A], initExecutor: ExecutionContext) extends Fiber[E, A] {

  sealed trait FiberState
  final case class Running(callbacks: List[Exit[E, A] => Any]) extends FiberState
  final case class Done(result: Exit[E, A]) extends FiberState

  type Erased       = ZIO[Any, Any, Any]
  type ErasedFold   = Fold[Any, Any, Any, Any, Any]
  type Continuation = Any => Erased

  var currentExecutor: ExecutionContext = initExecutor

  var currentZIO: Erased = erase(zio)

  val isFinalising =
    new AtomicBoolean(false)

  val isInterrupted =
    new AtomicBoolean(false)

  val isInterruptible =
    new AtomicBoolean(true)

  val continuationStack =
    new mutable.Stack[Continuation]()

  val environmentStack =
    new mutable.Stack[Any]()

  val state =
    new AtomicReference[FiberState](Running(List.empty))

  override def interrupt: ZIO[Any, Nothing, Unit] = {
    ZIO.succeedNow(isInterrupted.set(true))
  }

  override def join: ZIO[Any, E, A] =
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

  def erase[R, E1 >: E, B](zio: ZIO[R, E1, B]): Erased =
    zio.asInstanceOf[Erased]

  def findNextErrorHandler(): ErasedFold = {
    var loopStack                = true
    var errorHandler: ErasedFold = null
    while (loopStack) {
      if (continuationStack.isEmpty) {
        loopStack = false
      }
      else {
        continuationStack.pop() match {
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
      if (continuationStack.isEmpty) {
        loopStack = false
        complete(Exit.succeed(value.asInstanceOf[A]))
        println(s"[FiberContext] - complete with ${Exit.succeed(value.asInstanceOf[A])}")
      }
      else {
        val continuation = continuationStack.pop()
        currentZIO = continuation(value)
        println(s"[FiberContext] - continue with $currentZIO")
      }

    while (loopStack) {
      if (shouldInterrupt()) {
        isFinalising.set(true)
        continuationStack.push(_ => currentZIO)
        currentZIO = ZIO.failCause(Cause.Interrupt)
      }
      else {
        try {
          println(s"[FiberContext] - run current zio - $currentZIO")
          currentZIO match {

            case ZIO.Access(f) =>
              currentZIO = f(environmentStack.head)

            case ZIO.Async(register) =>
              loopStack = false
              if (continuationStack.isEmpty) {
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
                loopStack = false
                complete(Exit.fail(e().asInstanceOf[E]))
              }
              else {
                currentZIO = errorHandler.failure(e())
              }

            case ZIO.FlatMap(zio, continuation) =>
              continuationStack.push(continuation.asInstanceOf[Continuation])
              currentZIO = zio

            case fold @ ZIO.Fold(zio, _, _) =>
              continuationStack.push(fold)
              currentZIO = zio

            case ZIO.Fork(zio) =>
              val fiber = new FiberContext(zio, currentExecutor)
              continue(fiber)

            case ZIO.Provide(zio, environment) =>
              environmentStack.push(environment)
              currentZIO = zio.ensuring(ZIO.succeed(environmentStack.pop())).asInstanceOf[Erased]

            case ZIO.SetInterruptStatus(zio, status) =>
              val oldIsInterruptible = isInterruptible.get()
              isInterruptible.set(status.toBoolean)
              currentZIO = zio.ensuring(ZIO.succeed(isInterruptible.set(oldIsInterruptible)))

            case ZIO.Shift(executor) =>
              currentExecutor = executor
              continue(())

            case ZIO.Succeed(thunk) =>
              continue(thunk())

            case ZIO.SucceedNow(value) =>
              continue(value)
          }
        } catch {
          case t: Throwable =>
            currentZIO = ZIO.die(t)
        }
      }
    }
  }

  def shouldInterrupt(): Boolean =
    isInterruptible.get() && isInterrupted.get() && !isFinalising.get()

  currentExecutor.execute { () =>
    run()
  }
}
