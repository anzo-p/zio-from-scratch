import myZio.{ZIO, ZIOApp}

object AppInterruptability extends ZIOApp {

  val tick     = 20
  val repeat   = 4
  val lifetime = 100

  val unInterruptible =
    (ZIO.succeed(println("urgent uninterruptible"))
      *> ZIO.succeed(Thread.sleep(tick)))
      .repeatN(repeat)
      .unInterruptible

  // if lifetime < repeat * tick then we are interrupted before i have a chance to run
  val interruptible =
    (ZIO.succeed(println("interruptible process"))
      *> ZIO.succeed(Thread.sleep(tick))).forever

  val finalizer =
    ZIO.succeed(println("finalizing"))

  override def run: ZIO[Any, Nothing, Any] =
    for {
      fib <- (unInterruptible *> interruptible).ensuring(finalizer).fork
      _   <- ZIO.succeed(Thread.sleep(lifetime))
      _   <- fib.interrupt
    } yield ()
}
