import myZio.{ZIO, ZIOApp}

object AppBasicEffect extends ZIOApp {
  val lazyIO: ZIO[Any, Nothing, Unit] = ZIO.succeed(println("[App Main] - jazzy"))

  override def run: ZIO[Any, Nothing, Unit] = lazyIO
}
