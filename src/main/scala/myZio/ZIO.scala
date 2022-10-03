package myZio

import myZio.types.{Cause, Exit, InterruptStatus}

import java.util.concurrent.CountDownLatch
import scala.concurrent.ExecutionContext

sealed trait ZIO[-R, +E, +A] { self =>

  final private def unsafeRunFiber(implicit ev: Any <:< R): Fiber[E, A] =
    new FiberContext(self.asInstanceOf[ZIO[Any, E, A]], ZIO.defaultExecutor)

  final def unsafeRunSync(implicit ev: Any <:< R): Exit[E, A] = {
    var result = null.asInstanceOf[Exit[E, A]]
    val latch  = new CountDownLatch(1)

    val zio = self.foldCauseZIO(
      cause =>
        ZIO.succeed {
          result = Exit.failCause(cause)
          latch.countDown()
        },
      a =>
        ZIO.succeed {
          result = Exit.succeed(a)
          latch.countDown()
        }
    )

    zio.unsafeRunFiber
    latch.await()
    result
  }

  def as[B](a: => B): ZIO[R, E, B] =
    self.map(_ => a)

  def catchAll[R1 <: R, EU, A1 >: A](f: E => ZIO[R1, EU, A1]): ZIO[R1, EU, A1] =
    foldZIO(
      e => f(e),
      a => ZIO.succeedNow(a)
    )

  def ensuring[R1 <: R](finalizer: ZIO[R1, Nothing, Any]): ZIO[R1, E, A] =
    foldCauseZIO(
      cause => finalizer *> ZIO.failCause(cause),
      a => finalizer *> ZIO.succeedNow(a)
    )

  def flatMap[R1 <: R, E1 >: E, B](f: A => ZIO[R1, E1, B]): ZIO[R1, E1, B] =
    ZIO.FlatMap(self, f)

  def fold[B](failure: E => B, success: A => B): ZIO[R, E, B] =
    foldZIO(
      e => ZIO.succeedNow(failure(e)),
      a => ZIO.succeedNow(success(a))
    )

  def foldCauseZIO[R1 <: R, EU, B](failure: Cause[E] => ZIO[R1, EU, B], success: A => ZIO[R1, EU, B]): ZIO[R1, EU, B] =
    ZIO.Fold(self, failure, success)

  def foldZIO[R1 <: R, EU, B](failure: E => ZIO[R1, EU, B], success: A => ZIO[R1, EU, B]): ZIO[R1, EU, B] =
    foldCauseZIO(
      {
        case Cause.Fail(e) =>
          failure(e)
        case Cause.Die(throwable) =>
          ZIO.failCause(Cause.Die(throwable))
      },
      success
    )

  def forever: ZIO[R, E, Nothing] =
    self *> self.forever

  def fork: ZIO[R, Nothing, Fiber[E, A]] =
    ZIO.Fork(self)

  def interruptible: ZIO[R, E, A] =
    setInterruptStatus(InterruptStatus.Interruptible)

  def map[B](f: A => B): ZIO[R, E, B] =
    flatMap { a =>
      ZIO.succeedNow(f(a))
    }

  def provide(r: R): ZIO[Any, E, A] =
    ZIO.Provide(self, r)

  def repeatN(n: Int): ZIO[R, E, Unit] =
    if (n <= 0) ZIO.succeedNow()
    else self *> repeatN(n - 1)

  def setInterruptStatus(status: InterruptStatus): ZIO[R, E, A] =
    ZIO.SetInterruptStatus(self, status)

  def shift(executor: ExecutionContext): ZIO[R, Nothing, Unit] =
    ZIO.Shift(executor)

  def unInterruptible: ZIO[R, E, A] =
    setInterruptStatus(InterruptStatus.UnInterruptible)

  def zip[R1 <: R, E1 >: E, B](that: ZIO[R1, E1, B]): ZIO[R1, E1, (A, B)] =
    zipWith(that)(_ -> _)

  def zipPar[R1 <: R, E1 >: E, B](that: ZIO[R1, E1, B]): ZIO[R1, E1, (A, B)] =
    for {
      fib1 <- self.fork
      b    <- that
      a    <- fib1.join
    } yield (a, b)

  def zipRight[R1 <: R, E1 >: E, B](that: => ZIO[R1, E1, B]): ZIO[R1, E1, B] =
    zipWith(that)((_, b) => b)

  def zipWith[R1 <: R, E1 >: E, B, C](that: => ZIO[R1, E1, B])(f: (A, B) => C): ZIO[R1, E1, C] =
    for {
      a <- self
      b <- that
    } yield f(a, b)

  def *> [R1 <: R, E1 >: E, B](that: => ZIO[R1, E1, B]): ZIO[R1, E1, B] =
    self.zipRight(that)
}

object ZIO {
  private val defaultExecutor = ExecutionContext.global

  sealed trait ZIOPrimitive

  case class Access[R, E, A](f: R => ZIO[R, E, A]) extends ZIOPrimitive with ZIO[R, E, A]

  case class Async[A](register: (A => Any) => Any) extends ZIOPrimitive with ZIO[Any, Nothing, A]

  case class Fail[E](e: () => Cause[E]) extends ZIOPrimitive with ZIO[Any, E, Nothing]

  case class FlatMap[R, E, A, B](zio: ZIO[R, E, A], f: A => ZIO[R, E, B]) extends ZIOPrimitive with ZIO[R, E, B]

  case class Fold[R, E, EU, A, B](zio: ZIO[R, E, A], failure: Cause[E] => ZIO[R, EU, B], success: A => ZIO[R, EU, B])
      extends ZIOPrimitive
      with ZIO[R, EU, B]
      with (A => ZIO[R, EU, B]) {

    override def apply(a: A): ZIO[R, EU, B] = success(a)
  }

  case class Fork[R, E, A](zio: ZIO[R, E, A]) extends ZIOPrimitive with ZIO[R, Nothing, Fiber[E, A]]

  case class Provide[R, E, A](zio: ZIO[R, E, A], environment: R) extends ZIOPrimitive with ZIO[Any, E, A]

  case class SetInterruptStatus[R, E, A](self: ZIO[R, E, A], status: InterruptStatus)
      extends ZIOPrimitive
      with ZIO[R, E, A]

  case class Shift(executor: ExecutionContext) extends ZIOPrimitive with ZIO[Any, Nothing, Unit]

  case class Succeed[A](f: () => A) extends ZIOPrimitive with ZIO[Any, Nothing, A]

  case class SucceedNow[A](a: A) extends ZIOPrimitive with ZIO[Any, Nothing, A]

  def accessZIO[R, E, A](f: R => ZIO[R, E, A]): ZIO[R, E, A] =
    Access(f)

  def async[A](register: (A => Any) => Any): ZIO[Any, Nothing, A] =
    Async(register)

  def die(throwable: Throwable): ZIO[Any, Nothing, Nothing] =
    ZIO.failCause(Cause.Die(throwable))

  def done[E, A](exit: Exit[E, A]): ZIO[Any, E, A] =
    exit match {
      case Exit.Success(a) =>
        succeedNow(a)
      case Exit.Failure(e) =>
        failCause(e)
    }

  def fail[E](e: => E): ZIO[Any, E, Nothing] =
    failCause(Cause.Fail(e))

  def failCause[E](cause: Cause[E]): ZIO[Any, E, Nothing] =
    Fail(() => cause)

  def fromEither[E, A](either: Either[E, A]): ZIO[Any, E, A] =
    either.fold(
      e => fail(e),
      a => succeedNow(a)
    )

  def succeed[A](a: => A): ZIO[Any, Nothing, A] =
    Succeed(() => a)

  // should be private
  def succeedNow[A](a: A): ZIO[Any, Nothing, A] =
    SucceedNow(a)
}
