package myZio

sealed trait Cause[+E]

object Cause {
  final case class Fail[+E](error: E) extends Cause[E]
  final case class Die(throwable: Throwable) extends Cause[Nothing]
}

sealed trait Exit[+E, +A]

object Exit {
  final case class Failure[+E](e: Cause[E]) extends Exit[E, Nothing]
  final case class Success[+A](a: A) extends Exit[Nothing, A]

  def die(throwable: Throwable): Exit[Nothing, Nothing] =
    Failure(Cause.Die(throwable))

  def fail[E](e: E): Exit[E, Nothing] =
    Failure(Cause.Fail(e))

  def failCause[E](e: Cause[E]): Exit[E, Nothing] =
    Failure(e)

  def succeed[A](a: A): Exit[Nothing, A] =
    Success(a)
}
