import myZio.{ZIO, ZIOApp}

object AppZip extends ZIOApp {

  override def run: ZIO[Any, Nothing, (Int, String)] =
    ZIO.succeed(1).zip(ZIO.succeed("2"))
}
