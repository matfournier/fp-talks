// here's our definition of a monoid again
trait Monoid[A] {
  // an identity element
  def empty: A
  // an associative operation
  def combine(x: A, y: A): A
}

object Monoid {

  def apply[A](implicit monoid: Monoid[A]): Monoid[A] = monoid

  implicit val stringMonoid: Monoid[String] = new Monoid[String] {
    val empty: String = ""
    def combine(x: String, y: String): String = x + y
  }

  // typeclass composition!
  // here's where things start to get more interesting, this says "I
  // can give you a monoid for any Option[A] if you can give me a
  // monoid for A", I never have to define monoid for Option[Int], Option[String], etc.
  implicit def optionMonoid[A](implicit am: Monoid[A]): Monoid[Option[A]] =
    new Monoid[Option[A]] {
      val empty: Option[A] = None

      def combine(x: Option[A], y: Option[A]): Option[A] = (x, y) match {
        case (Some(_), None) => x
        case (None, Some(_)) => y
        case (Some(xx), Some(yy)) => Some(am.combine(xx, yy)) // here we use the other monoid to combine them
        case _ =>
          None
      }
    }

  // typeclass composition!
  // given an monoid for B, I can give you a monoid for functions
  // returning B, by running the functions on the input and adding the
  // results
  // note we use a summoning implicit here based on the apply method we added to the companion object
  // to have having to right Implicitly[Monoid[B]].combine, or having to put implicit bm: Monoid[B] into
  // the implicit parameter part of the function
  implicit def functionMonoid[A, B: Monoid]: Monoid[A => B] =
    new Monoid[A => B] {
      def empty: A => B = _ => {
        Monoid[B].empty
      }

      def combine(x: A => B, y: A => B): A => B = { a =>
        Monoid[B].combine(x(a), y(a))
      }
    }


  // we can use a monoid to collapse a bunch of values, here we take a
  // list and function that takes us to a value for which we have a
  // Monoid, and we can then collapse the list into a single value.
  implicit def foldRight[A](la: List[A])(implicit am: Monoid[A]): A =
    la.foldRight(am.empty)(am.combine)

  implicit def foldLeft[A](la: List[A])(implicit am: Monoid[A]): A =
    la.foldLeft(am.empty)(am.combine)
}

object Example {
  import Monoid._  // import our typeclasses

  // we'll start with some functions
  val fizz: Int => Option[String] = x => if (x % 3 == 0) Some("fizz") else None
  val buzz: Int => Option[String] = x => if (x % 5 == 0) Some("buzz") else None
  val bazz: Int => Option[String] = x => if (x % 7 == 0) Some("bazz") else None

  val funcs = List(fizz, buzz, bazz)

  //   we can combine our functions, this works because we can find an
  //   option monoid for strings, since we have a monoid for strings,
  //   then we can find a monoid for Int => Option[String] since we now
  //   have a monoid for Option[String]
  val fizzbuzzbazz = foldRight(funcs)

  // handle the Nones
  val fbbOrInt: Int => String = { i =>
    (fizzbuzzbazz(i) getOrElse i.toString) + ","
  }

  // map our function on a list
  val strings: List[String] = (1 until 100).toList map fbbOrInt

}

object main extends App {
  import Monoid._
  import Example._

  println(foldRight(strings))
}
