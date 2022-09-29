import zio.{ZIO, ZIOApp}

object AppZip extends ZIOApp {
  override def run: ZIO[(Int, String)] =
    ZIO.succeed(1).zip(ZIO.succeed("2"))
}
