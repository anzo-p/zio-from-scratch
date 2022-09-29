package myZio

import scala.concurrent.ExecutionContext

sealed trait Fiber[+A] {

  def join: ZIO[A]

  def start(): Unit
}

class FiberImpl[A](zio: ZIO[A]) extends Fiber[A] {

  var maybeResult: Option[A] = None

  var callbacks = List.empty[A => Any]

  override def join: ZIO[A] =
    maybeResult match {
      case None =>
        ZIO.async { complete =>
          callbacks = complete :: callbacks
        }

      case Some(result) =>
        ZIO.succeedNow(result)
    }

  override def start(): Unit =
    ExecutionContext.global.execute { () =>
      zio.run { result =>
        maybeResult = Some(result)
        callbacks.foreach { callback =>
          callback(result)
        }
      }
    }
}
