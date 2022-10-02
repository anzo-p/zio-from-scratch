package myZio

sealed trait Cause[+E]

object Cause {
  final case class Fail[+E](error: E) extends Cause[E] // expected errors, some of which might want to recover from
  final case class Die(throwable: Throwable) extends Cause[Nothing] // unexpected and unrecoverable errors
}
