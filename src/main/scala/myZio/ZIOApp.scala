package myZio

trait ZIOApp {

  def run: ZIO[Any, Any]

  def main(args: Array[String]): Unit = {
    val result = run.unsafeRunSync
    println(s"[ZIOApp Main] - ZIO evaluated and the result is $result")
  }
}
