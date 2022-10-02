package myZio

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
