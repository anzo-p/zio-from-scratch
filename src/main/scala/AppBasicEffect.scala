import myZio.{ZIO, ZIOApp}

object AppBasicEffect extends ZIOApp {
  val lazyIO: ZIO[Nothing, Unit] = ZIO.succeed(println("jazzy"))

  override def run: ZIO[Nothing, Unit] = lazyIO
}
