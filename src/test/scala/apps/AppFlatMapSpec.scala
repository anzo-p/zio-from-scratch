package apps

import models.Cat
import myZio.ZIO
import utils.ArbitraryTestData.sample
import utils.BaseSpec

class AppFlatMapSpec extends BaseSpec {

  "FlatMap" should {

    "flatMap over ZIO of scalar" in {
      val testValue    = sample[Int]
      val testZio      = ZIO.succeed(testValue)
      val testFunction = (a: Int) => ZIO.succeed(a + 1)
      val expected     = testFunction(testValue)

      testZio.flatMap(testFunction).run { testResult =>
        expected.run { expectedResult =>
          testResult mustBe expectedResult
        }
      }
    }

    "flatMap over ZIO of option" in {
      val testValue    = Option(sample[Int])
      val testZio      = ZIO.succeed(testValue)
      val testFunction = (a: Option[Int]) => ZIO.succeed(a.map(_ + 1))
      val expected     = testFunction(testValue)

      testZio.flatMap(testFunction).run { testResult =>
        expected.run { expectedResult =>
          testResult mustBe expectedResult
        }
      }
    }

    "flatMap over ZIO of sequence" in {
      val testValues   = Seq(sample[Int], sample[Int], sample[Int])
      val testZio      = ZIO.succeed(testValues)
      val testFunction = (xs: Seq[Int]) => ZIO.succeed(xs.flatMap(x => Seq(x - 1, x + 1)))
      val expected     = testFunction(testValues)

      testZio.flatMap(testFunction).run { testResult =>
        expected.run { expectedResult =>
          expectedResult mustBe testResult
        }
      }
    }

    "support for comprehension" in {
      val testName = sample[String]
      val testAge  = sample[Int]
      val expected = Cat(testName, testAge)

      val result: ZIO[Cat] = for {
        name <- ZIO.succeed(testName)
        age  <- ZIO.succeed(testAge)
      } yield Cat(name, age)

      result.run { result =>
        result mustBe expected
      }
    }
  }
}
