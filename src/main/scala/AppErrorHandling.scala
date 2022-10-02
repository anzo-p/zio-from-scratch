import myZio.{ZIO, ZIOApp}

object AppErrorHandling extends ZIOApp {

  val failedProgram: ZIO[String, Unit] =
    ZIO
      .fail("Failed..")
      .flatMap(_ => ZIO.succeed(println("[App Main] - I must never echo")))
      .catchAll(e => ZIO.succeed(println(s"[App Main] - Recovered from an error: $e")))
      .flatMap(_ => ZIO.succeed(println("[App Main] - Still trying past catch all")))
      .catchAll(_ =>
        ZIO.succeed(println("[App Main] - The second error catch all mustn't echo because no subsequent errors..")))

  override def run: ZIO[Any, Any] = failedProgram
}
