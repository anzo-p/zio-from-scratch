package myZio

import myZio.InterruptStatus._

sealed trait InterruptStatus { self =>

  def toBoolean: Boolean =
    self match {
      case Interruptible =>
        true
      case UnInterruptible =>
        false
    }
}

object InterruptStatus {
  case object Interruptible extends InterruptStatus

  case object UnInterruptible extends InterruptStatus
}
