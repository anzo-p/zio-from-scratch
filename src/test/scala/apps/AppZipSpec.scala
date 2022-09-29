package apps

import utils.ArbitraryTestData.sample
import utils.BaseSpec
import zio.ZIO

class AppZipSpec extends BaseSpec {

  "Zip" should {
    val fst     = sample[Int]
    val snd     = sample[String]
    val testZio = ZIO.succeed(fst).zip(ZIO.succeed(snd))

    "make a tuple out of two ZIOs in their Zipping order" in {
      testZio.run { result =>
        result.toString mustBe (fst, snd).toString
      }
    }
  }
}
