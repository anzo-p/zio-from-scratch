package myZio.types

sealed trait Cause[+E]

object Cause {
  final case class Fail[+E](error: E) extends Cause[E]
  final case class Die(throwable: Throwable) extends Cause[Nothing]
  final case object Interrupt extends Cause[Nothing]
}
