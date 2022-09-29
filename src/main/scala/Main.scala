sealed trait ZIO[+A] { self =>
  def run(callback: A => Unit): Unit
}

object ZIO {
  private case class Effect[A](f: () => A) extends ZIO[A] {
    override def run(callback: A => Unit): Unit =
      callback(f())
  }

  private case class Succeed[A](value: A) extends ZIO[A] {
    override def run(callback: A => Unit): Unit =
      callback(value)
  }

  def succeedNow[A](value: A): ZIO[A] =
    ZIO.Succeed(value)

  def succeed[A](value: => A): ZIO[A] =
    ZIO.Effect(() => value)
}

trait ZIOApp {
  def run: ZIO[Any]

  def main(args: Array[String]): Unit =
    run.run { result =>
      println(s"ZIO evaluated and the result is $result")
    }
}

// ---------- MODULE CHANGE ----------

case class Cat(name: String, age: Int)

object AppEffect extends ZIOApp {
  val eagerCat: ZIO[Cat] = ZIO.succeedNow(Cat("jizzy", 5)) // wont print

  // eager IO not wanted, the effect couldnt catch exceptions and also introduces a bug space of IO of unexpected timing
  val eagerIO: ZIO[Unit] = ZIO.succeedNow(println("unintentional IO")) // should intentionally print

  val lazyIO: ZIO[Unit] = ZIO.succeed(println("jazzy")) // only publish lazy evaluations

  override def run: ZIO[Unit] = lazyIO
}

// -----
object TheSimplestCallback extends App {
  trait Animal
  case class Cat(name: String, age: Int) extends Animal

  val cat = Cat("jizzy", 5)

  def eval(f: Animal => Unit): Unit = {
    f(cat)
  }

  eval(println)
  eval(a => println(s"program evaluated and the result is $a"))
}
