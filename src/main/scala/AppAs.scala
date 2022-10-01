import models.Cat
import myZio.{ZIO, ZIOApp}

object AppAs extends ZIOApp {

  override def run: ZIO[Nothing, Any] =
    ZIO
      .succeed(Cat("lizzy", 3))
      .flatMap { cat =>
        ZIO.succeed(println(s"cheers to $cat"))
      }
      .as("awesome") // same as .map(_ => "awesome") or for { .. } yield "awesome"
}
