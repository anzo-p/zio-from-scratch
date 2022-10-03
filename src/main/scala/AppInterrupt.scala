import myZio.{ZIO, ZIOApp}

object AppInterrupt extends ZIOApp {

  val program: ZIO[Nothing, Unit] =
    (ZIO.succeed(println("[App Main] 10110011 11110001 00100001 00100110"))
      *> ZIO.succeed(Thread.sleep(300)))

  override def run: ZIO[Any, Any] =
    for {
      fib <- program
              .forever
              .ensuring(
                ZIO.succeed(println("[App Main] we are being interrupted so lets finalize and close all resources")))
              .fork
      _ <- ZIO.succeed(Thread.sleep(2000))
      _ <- fib.interrupt
    } yield ()
}
