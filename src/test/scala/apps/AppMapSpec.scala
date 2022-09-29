package apps

import models.Cat
import utils.BaseSpec
import myZio.ZIO
import utils.ArbitraryTestData.sample

class AppMapSpec extends BaseSpec {

  "Map" should {

    "map over scalar" in {
      val testValue = sample[Int]
      val testZio   = ZIO.succeed(testValue)
      val expected  = testValue + 1

      testZio.map(_ + 1).run { result =>
        result mustBe expected
      }
    }

    "map over tuple" in {
      val name     = sample[String]
      val age      = sample[Int]
      val testZio  = ZIO.succeed(name).zip(ZIO.succeed(age))
      val expected = Cat(name, age)

      testZio
        .map {
          case (name, age) =>
            Cat(name, age)
        }
        .run { result =>
          result mustBe expected
        }
    }

    "map over option" in {
      val testValues   = Option(sample[Int])
      val testZio      = ZIO.succeed(testValues)
      val testFunction = (a: Int) => a + 1
      val expected     = testValues.map(testFunction)

      testZio.map(xs => xs.map(testFunction)).run { result =>
        result mustBe expected
      }
    }

    "map over sequence" in {
      val testValues   = Seq(sample[Int], sample[Int], sample[Int])
      val testZio      = ZIO.succeed(testValues)
      val testFunction = (a: Int) => a + 1
      val expected     = testValues.map(testFunction)

      testZio.map(xs => xs.map(testFunction)).run { result =>
        result mustBe expected
      }
    }
  }
}
