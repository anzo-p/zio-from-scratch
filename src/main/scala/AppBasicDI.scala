import myZio.{ZIO, ZIOApp}

object AppBasicDI extends ZIOApp {

  val zio =
    ZIO.accessZIO[Int, Nothing, Unit] { k =>
      ZIO.succeed(println(s"I got value $k"))
    }

  val zio2: ZIO[Any, Nothing, Unit] =
    zio.provide(42)

  override def run =
    zio2
}
