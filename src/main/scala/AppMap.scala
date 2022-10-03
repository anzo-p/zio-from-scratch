import models.Cat
import myZio.{ZIO, ZIOApp}

object AppMap extends ZIOApp {

  val zioTuple: ZIO[Any, Nothing, (Int, String)] = ZIO.succeed(5).zip(ZIO.succeed("loki"))

  override def run: ZIO[Any, Nothing, Cat] =
    zioTuple
      .map {
        case (age, name) =>
          Cat(name, age)
      }
}
