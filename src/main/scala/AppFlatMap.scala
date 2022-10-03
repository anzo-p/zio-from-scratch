import models.Cat
import myZio.{ZIO, ZIOApp}

object AppFlatMap extends ZIOApp {
  val zioTuple: ZIO[Any, Nothing, (Int, String)] = ZIO.succeed(10).zip(ZIO.succeed("charlie"))

  override def run: ZIO[Any, Nothing, Any] =
    for {
      _ <- zioTuple.flatMap { tuple =>
            ZIO.succeed(println(s"[App Main] - cheers to the tuple $tuple"))
          }

      name <- ZIO.succeed("bandit")
      age  <- ZIO.succeed(1)
      _    <- ZIO.succeed(name).zip(ZIO.succeed(age))
    } yield Cat(name, age)
}
