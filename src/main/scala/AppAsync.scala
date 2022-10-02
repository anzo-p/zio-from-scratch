import myZio.{ZIO, ZIOApp}

object AppAsync extends ZIOApp {

  override def run: ZIO[Nothing, Int] =
    ZIO.async[Int] { complete =>
      println("[App Main] - async begins")
      Thread.sleep(1000)
      complete(42)
    }
}
