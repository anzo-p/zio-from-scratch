package myZio

import myZio.ZIO.defaultExecutor

import java.util.concurrent.CountDownLatch
import scala.concurrent.ExecutionContext

sealed trait ZIO[+A] { self =>

  final private def unsafeRunFiber(): Fiber[A] =
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

  def as[B](value: => B): ZIO[B] =
    self.map(_ => value)

  def flatMap[B](f: A => ZIO[B]): ZIO[B] =
    ZIO.FlatMap(self, f)

  def fork: ZIO[Fiber[A]] =
    ZIO.Fork(self)

  def map[B](f: A => B): ZIO[B] =
    flatMap { a =>
      ZIO.succeedNow(f(a))
    }

  def repeatN(n: Int): ZIO[Unit] =
    if (n <= 0) ZIO.succeedNow()
    else self *> repeatN(n - 1)

  def shift(executor: ExecutionContext): ZIO.Shift =
    ZIO.Shift(executor)

  def zip[B](that: ZIO[B]): ZIO[(A, B)] =
    zipWith(that)(_ -> _)

  def zipPar[B](that: ZIO[B]): ZIO[(A, B)] =
    for {
      fib1 <- self.fork
      b    <- that
      a    <- fib1.join
    } yield (a, b)

  def zipRight[B](that: => ZIO[B]): ZIO[B] =
    zipWith(that)((_, b) => b)

  def zipWith[B, C](that: => ZIO[B])(f: (A, B) => C): ZIO[C] =
    for {
      a <- self
      b <- that
    } yield f(a, b)

  def *> [B](that: => ZIO[B]): ZIO[B] =
    self.zipRight(that)
}

object ZIO {
  private val defaultExecutor = ExecutionContext.global

  case class Async[A](register: (A => Any) => Any) extends ZIO[A]

  case class Effect[A](f: () => A) extends ZIO[A]

  case class FlatMap[A, B](zio: ZIO[A], f: A => ZIO[B]) extends ZIO[B]

  case class Fork[A](zio: ZIO[A]) extends ZIO[Fiber[A]]

  case class Shift(executor: ExecutionContext) extends ZIO[Unit]

  case class Succeed[A](value: A) extends ZIO[A]

  // should be private
  def succeedNow[A](value: A): ZIO[A] =
    ZIO.Succeed(value)

  def async[A](register: (A => Any) => Any): ZIO[A] =
    ZIO.Async(register)

  def succeed[A](value: => A): ZIO[A] =
    ZIO.Effect(() => value)
}
