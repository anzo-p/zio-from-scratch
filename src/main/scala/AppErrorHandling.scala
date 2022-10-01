import myZio.{ZIO, ZIOApp}

object AppErrorHandling extends ZIOApp {

  val failedProgram: ZIO[String, Unit] =
    ZIO
      .fail("Failed..")
      .flatMap(_ => ZIO.succeed(println("I must never echo")))
      .catchAll(e => ZIO.succeed(println(s"Recovered from an error: $e")))
      .flatMap(_ => ZIO.succeed(println("Still trying past catch all")))
      .catchAll(_ => ZIO.succeed(println("The second error catch all mustn't echo because no subsequent errors..")))

  override def run: ZIO[Any, Any] = failedProgram
}
