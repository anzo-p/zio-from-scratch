package myZio

trait ZIOApp {
  def run: ZIO[Any]

  def main(args: Array[String]): Unit =
    run.run { result =>
      println(s"ZIO evaluated and the result is $result")
    }
}
