import myZio.{ZIO, ZIOApp}

import scala.util.Random

object AppZipPar extends ZIOApp {

  override val await = 3000

  def asyncZIO(n: Int): ZIO[Int] = ZIO.async[Int] { complete =>
    println(s"async $n begins")
    Thread.sleep(1000 + Random.nextInt(1000))
    println(s"async $n has result")
    complete(Random.nextInt())
  }

  override def run: ZIO[Any] =
    asyncZIO(1).zipPar(asyncZIO(2))
}
