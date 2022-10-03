import models.Cat
import myZio.{ZIO, ZIOApp}

object AppAs extends ZIOApp {

  override def run: ZIO[Any, Nothing, Any] =
    ZIO
      .succeed(Cat("lizzy", 3))
      .flatMap { cat =>
        ZIO.succeed(println(s"[App Main] - cheers to $cat"))
      }
      .as("awesome") // same as .map(_ => "awesome") or for { .. } yield "awesome"
}
