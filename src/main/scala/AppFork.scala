import myZio.{ZIO, ZIOApp}

import scala.util.Random

object AppFork extends ZIOApp {

  def asyncZIO(n: Int): ZIO[Int] = ZIO.async[Int] { complete =>
    println(s"async $n begins")
    Thread.sleep(1000 + Random.nextInt(1000))
    println(s"async $n has result")
    complete(Random.nextInt())
  }

  override def run: ZIO[String] =
    for {
      fib1 <- asyncZIO(1).fork
      fib2 <- asyncZIO(2).fork
      fib3 <- asyncZIO(3).fork

      ans1 <- fib1.join
      ans2 <- fib2.join
      ans3 <- fib3.join

    } yield s"reduced into $ans1, $ans2, and $ans3"
}
