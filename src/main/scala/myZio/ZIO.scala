package myZio

sealed trait ZIO[+A] { self =>

  def run(callback: A => Unit): Unit

  def as[B](value: => B): ZIO[B] =
    self.map(_ => value)

  def flatMap[B](f: A => ZIO[B]): ZIO[B] =
    ZIO.FlatMap(self, f)

  def map[B](f: A => B): ZIO[B] =
    flatMap { a =>
      ZIO.succeedNow(f(a))
    }

  def zip[B](that: ZIO[B]): ZIO[(A, B)] = {
    for {
      a <- self
      b <- that
    } yield (a, b)
  }
}

object ZIO {
  private case class Effect[A](f: () => A) extends ZIO[A] {
    override def run(callback: A => Unit): Unit =
      callback(f())
  }

  private case class FlatMap[A, B](zio: ZIO[A], f: A => ZIO[B]) extends ZIO[B] {
    override def run(callback: B => Unit): Unit =
      zio.run { a =>
        f(a).run(callback)
      }
  }

  private case class Succeed[A](value: A) extends ZIO[A] {
    override def run(callback: A => Unit): Unit =
      callback(value)
  }

  def succeedNow[A](value: A): ZIO[A] =
    ZIO.Succeed(value)

  def succeed[A](value: => A): ZIO[A] =
    ZIO.Effect(() => value)
}
