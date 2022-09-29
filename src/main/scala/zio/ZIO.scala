package zio

sealed trait ZIO[+A] { self =>
  def run(callback: A => Unit): Unit
}

object ZIO {
  private case class Effect[A](f: () => A) extends ZIO[A] {
    override def run(callback: A => Unit): Unit =
      callback(f())
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
