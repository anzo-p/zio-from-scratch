import models.Cat
import myZio.{ZIO, ZIOApp}

object AppBasicEffect extends ZIOApp {
  val eagerCat: ZIO[Cat] = ZIO.succeedNow(Cat("jizzy", 5)) // mustn't print

  // eager IO not wanted, the effect couldn't catch exceptions and also introduces a bug space of IO of unexpected timing
  val eagerIO: ZIO[Unit] = ZIO.succeedNow(println("unintentional IO")) // should intentionally print

  val lazyIO: ZIO[Unit] = ZIO.succeed(println("jazzy")) // must print, but will do so lazily

  override def run: ZIO[Unit] = lazyIO
}
