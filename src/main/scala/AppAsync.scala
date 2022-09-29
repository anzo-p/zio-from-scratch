import myZio.{ZIO, ZIOApp}

object AppAsync extends ZIOApp {

  override def run: ZIO[Int] = ZIO.async[Int] { complete =>
    println("async begins")
    Thread.sleep(1000)
    complete(42)
  }
}
