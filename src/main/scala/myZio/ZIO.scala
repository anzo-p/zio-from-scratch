package myZio

import myZio.ZIO.defaultExecutor

import java.util.concurrent.CountDownLatch
import scala.concurrent.ExecutionContext

sealed trait ZIO[+E, +A] { self =>

  final private def unsafeRunFiber(): Fiber[E, A] =
    new FiberContext(self, defaultExecutor)

  final def unsafeRunSync: A = {
    var result: A = null.asInstanceOf[A]
    val latch     = new CountDownLatch(1)

    val zio = self.flatMap { a =>
      ZIO.succeed {
        result = a
        latch.countDown()
      }
    }

    zio.unsafeRunFiber()
    latch.await()
    result
  }

  def as[B](value: => B): ZIO[E, B] =
    self.map(_ => value)

  def flatMap[E1 >: E, B](f: A => ZIO[E1, B]): ZIO[E1, B] =
    ZIO.FlatMap(self, f)

  def fork: ZIO[Nothing, Fiber[E, A]] =
    ZIO.Fork(self)

  def map[B](f: A => B): ZIO[E, B] =
    flatMap { a =>
      ZIO.succeedNow(f(a))
    }

  def repeatN(n: Int): ZIO[E, Unit] =
    if (n <= 0) ZIO.succeedNow()
    else self *> repeatN(n - 1)

  def shift(executor: ExecutionContext): ZIO[Nothing, Unit] =
    ZIO.Shift(executor)

  def zip[E1 >: E, B](that: ZIO[E1, B]): ZIO[E1, (A, B)] =
    zipWith(that)(_ -> _)

  def zipPar[E1 >: E, B](that: ZIO[E1, B]): ZIO[E1, (A, B)] =
    for {
      fib1 <- self.fork
      b    <- that
      a    <- fib1.join
    } yield (a, b)

  def zipRight[E1 >: E, B](that: => ZIO[E1, B]): ZIO[E1, B] =
    zipWith(that)((_, b) => b)

  def zipWith[E1 >: E, B, C](that: => ZIO[E1, B])(f: (A, B) => C): ZIO[E1, C] =
    for {
      a <- self
      b <- that
    } yield f(a, b)

  def *> [E1 >: E, B](that: => ZIO[E1, B]): ZIO[E1, B] =
    self.zipRight(that)
}

object ZIO {
  private val defaultExecutor = ExecutionContext.global

  case class Async[A](register: (A => Any) => Any) extends ZIO[Nothing, A]

  case class FlatMap[E, A, B](zio: ZIO[E, A], f: A => ZIO[E, B]) extends ZIO[E, B]

  case class Fork[E, A](zio: ZIO[E, A]) extends ZIO[Nothing, Fiber[E, A]]

  case class Shift(executor: ExecutionContext) extends ZIO[Nothing, Unit]

  case class Succeed[A](f: () => A) extends ZIO[Nothing, A]

  case class SucceedNow[A](value: A) extends ZIO[Nothing, A]

  def async[A](register: (A => Any) => Any): ZIO[Nothing, A] =
    ZIO.Async(register)

  def succeed[A](value: => A): ZIO[Nothing, A] =
    ZIO.Succeed(() => value)

  // should be private
  def succeedNow[A](value: A): ZIO[Nothing, A] =
    ZIO.SucceedNow(value)
}
