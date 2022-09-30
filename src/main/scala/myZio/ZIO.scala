package myZio

import scala.collection.mutable

sealed trait ZIO[+A] { self =>

  final def run(callback: A => Unit): Unit = {

    type Erased = ZIO[Any]

    type ErasedCallback = Any => Unit

    type Continuation = Any => Erased

    def erase[B](zio: ZIO[B]): Erased =
      zio

    def eraseCallback[B](cb: B => Unit): ErasedCallback =
      cb.asInstanceOf[ErasedCallback]

    val stack = new mutable.Stack[Continuation]()

    var currentZIO = erase(self)

    var loop = true

    def resume(): Unit = {
      loop = true
      run()
    }

    def stop(): Unit =
      loop = false

    def complete(value: Any): Unit =
      if (stack.isEmpty) {
        stop()
        callback(value.asInstanceOf[A])
      }
      else {
        val continuation = stack.pop()
        currentZIO = continuation(value)
      }

    def run(): Unit =
      while (loop) {
        currentZIO match {

          case ZIO.Async(register) =>
            if (stack.isEmpty) {
              stop()
              register(eraseCallback(callback))
            }
            else {
              stop() // stop our own stack runner while 3rd party callback is running
              register { a =>
                currentZIO = ZIO.succeedNow(a)
                resume()
              }
            }

          case ZIO.Effect(thunk) =>
            complete(thunk())

          case ZIO.FlatMap(zio, continuation) =>
            stack.push(continuation.asInstanceOf[Continuation])
            currentZIO = zio

          case ZIO.Fork(zio) =>
            val fiber = new FiberImpl(zio)
            fiber.start()
            complete(fiber)

          case ZIO.Succeed(value) =>
            complete(value)
        }
      }

    run()
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
  private case class Async[A](register: (A => Any) => Any) extends ZIO[A]

  private case class Effect[A](f: () => A) extends ZIO[A]

  private case class FlatMap[A, B](zio: ZIO[A], f: A => ZIO[B]) extends ZIO[B]

  private case class Fork[A](zio: ZIO[A]) extends ZIO[Fiber[A]]

  private case class Succeed[A](value: A) extends ZIO[A]

  // should be private
  def succeedNow[A](value: A): ZIO[A] =
    ZIO.Succeed(value)

  def async[A](register: (A => Any) => Any): ZIO[A] =
    ZIO.Async(register)

  def succeed[A](value: => A): ZIO[A] =
    ZIO.Effect(() => value)
}
