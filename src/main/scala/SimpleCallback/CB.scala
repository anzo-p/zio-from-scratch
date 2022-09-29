package SimpleCallback

import models._

object CB extends App {

  val cat = Cat("jizzy", 5)

  def eval(f: Animal => Unit): Unit = {
    f(cat)
  }

  eval(println)
  eval(a => println(s"program evaluated and the result is $a"))
}
