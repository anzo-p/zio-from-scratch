import myZio.{ZIO, ZIOApp}

object AppExceptionHandling extends ZIOApp {
  override def run: ZIO[Any, Nothing, Int] =
    ZIO
      .succeed { throw new NoSuchMethodException("abcde") }
      .catchAll { _ =>
        ZIO.succeed(println(s"[App Main] - I cannot echo until there is an error handler to catch my type"))
      }
      .foldCauseZIO(
        _ =>
          ZIO.succeed(println(s"[App Main] - Recovered from a Cause because case catchAll couldn't"))
            *> ZIO.succeed(1),
        _ => ZIO.succeed(0)
      )
}
