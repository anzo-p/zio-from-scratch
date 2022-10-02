import myZio.{ZIO, ZIOApp}

object AppBasicEffect extends ZIOApp {
  val lazyIO: ZIO[Nothing, Unit] = ZIO.succeed(println("[App Main] - jazzy"))

  override def run: ZIO[Nothing, Unit] = lazyIO
}
