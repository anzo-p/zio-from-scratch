package utils

import org.scalacheck.Arbitrary
import org.scalacheck.Arbitrary.arbitrary

object ArbitraryTestData {

  def sample[A : Arbitrary]: A = arbitrary[A].sample.get
}
