import myZio.{ZIO, ZIOApp}

object AppStackSafe extends ZIOApp {

  override def run: ZIO[Nothing, Any] =
    ZIO.succeed(println("[App Main] - a")).repeatN(1000000)
}
