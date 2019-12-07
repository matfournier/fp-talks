import cats._
import cats.implicits._

object FizzbuzzCats extends App {
  val fizz: Int => Option[String] = x => if (x % 3 == 0) Some("fizz") else None
  val buzz: Int => Option[String] = x => if (x % 5 == 0) Some("buzz") else None
  val bazz: Int => Option[String] = x => if (x % 7 == 0) Some("bazz") else None
  val funcs = List(fizz, buzz, bazz)
  // we'll fold them together (combineAll is just foldLeft for monoids) (whaaaa...)
  val fizzbuzzbazz = funcs.combineAll
  // handle the Nones
  val fbbOrInt: Int => String = { i =>
    (fizzbuzzbazz(i) getOrElse i.toString) + ","
  }
  // generate the first 50 results
  val strings: List[String] = (1 until 50).toList map fbbOrInt

  println(strings.combineAll)
}