import myZio.{ZIO, ZIOApp}

import scala.util.Random

object AppZipPar extends ZIOApp {

  def asyncZIO(n: Int): ZIO[Nothing, Int] = ZIO.async[Int] { complete =>
    println(s"[App Main] - async $n begins")
    Thread.sleep(1000 + Random.nextInt(1000))
    println(s"[App Main] - async $n has result")
    complete(Random.nextInt())
  }

  override def run: ZIO[Nothing, Any] =
    asyncZIO(1).zipPar(asyncZIO(2))
}
