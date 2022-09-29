import myZio.{ZIO, ZIOApp}

object AppBasicEffect extends ZIOApp {
  // eager IO not wanted, ZIO couldn't catch exceptions,
  // also introduces a bug space of unexpected IO timing
  //val eagerIO: ZIO[Unit] = ZIO.succeedNow(println("unintentional IO"))

  val lazyIO: ZIO[Unit] = ZIO.succeed(println("jazzy"))

  override def run: ZIO[Unit] = lazyIO
}
