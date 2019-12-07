---
title: Typeclasses 
author: Mathew Fournier
patat:
  theme:
    syntaxHighlighting:
      decVal: [bold]
  images:
    backend: auto
...

# Typeclasses

- invented in the 89/90?ish to solve the problem of method overloading in Haskell 
- superficially from the 30,000 ft level look like interfaces 
    - some can be implemented in java, but not all typeclasses can be implemented in an OO lang like java
- scala does not have first class typeclasses like haskell / Rust
    - the _class_ part has nothing to do with an OO class and is an unfortunate name

Languages with typeclasses: 

- Haskell 
- PureScript 
- Rust
- Scala (via implicits) 

---

# Typeclasses

Why? 

- they allow us to extend libraries with new functionality 
- they do not use traditional inheritance (subtyping), even though there is kind of a hierarchy of inheritance
- you can add new functionality w/o changing original source code
    - see expression problem

It's better to think about it as _type capabilities_ or _type behaviors_ and ignore the _class_ part

---

# Typeclasses - Interfaces

People will try to say that `Interfaces` are like typeclasses. While there are some similarities, it's not really true.
See:

- [typeclasses are nothing like interfaces](https://blog.tmorris.net/posts/type-classes-are-nothing-like-interfaces/)
- [how to typeclasses differe from interfaces](https://www.parsonsmatt.org/2017/01/07/how_do_type_classes_differ_from_interfaces.html)
- [typeclasses in translation](https://joyofhaskell.com/posts/2017-03-15-typeclasses-in-translation.html)

tldr:

- separation of implementation (we'll get to this)
- return type polymorphism (not in scope of this talk)

---

# Typeclass example 

```scala
object Thing {
    def doThing[F[_]: Monad](a: F[String], b: F[Long], c: F[Double]): F[User] = for {
        aa <- a
        bb <- b
        cc <- c
    } yield User(aa, bb, cc)
}
```

- also the `Reads` / `Writes` / `Format` in `Play-Json`

---

# Typeclass

A typeclass in Scala is a trait that:

* holds no state
* has a type parameter (can be a higher kinded type)
* has at least one abstract method
* _may_ contain generalized methods
* _may_ may extend other typeclsases

There should be only _one implementation_ of a typeclass for any given type parameter

* this is known as _typeclass coherence_
* scala will let you define more than one, but if they are both in scope == compile error
* having _more than one_ is a bad design


---

# Typeclasses - Languages

A typeclass has a typeclass definition and one or more implementations:

```scala
// scala
trait Show[T] {
  def show(v: T): String 
}

implicit def showString = new Show[String] {
   def show(v: String): String = v 
}
```

```rust
// rust
pub trait Show {
  fn show(&self) -> String;
}

impl Show for string {
  fn show(&self) -> String {
    self
  }
}

```

---

# Typeclasses Language 

```scala
// scala
trait Show[T] {
  def show(v: T): String 
}

implicit def showString = new Show[String] {
   def show(v: String): String = v 
}
```

```haskell
-- haskell
class Show a where
  show :: a -> String 

instance Show String where 
  show s = s 
``` 
---

# Typeclasses - you see this all the time

You see these everywhere in Scala.  For example, `scala.math.Numeric`:

```scala
trait Ordering[T] {
  def compare(x: T, y: T): Int
  def lt(x: T, y: T): Boolean = compare(x, y) < 0
  def gt(x: T, y: T): Boolean = compare(x, y) > 0
}

trait Numeric[T] extends Ordering[T] {
  def plus(x: T, y: T): T
  def times(x: T, y: T): T
  def negate(x: T): T
  def zero: T
  def abs(x: T): T = if (lt(x, zero)) negate(x) else x
}

```

---

# Typeclass - using one

```scala
def signOfTheTimes[T](t: T)(implicit N: Numeric[T]): T = {
  import N._
  times(negate(abs(t)), t)
}
```

Ignoring the (for now) horrible syntax:

- we no longer have an OOP hierarchy for our input types
- our type no longer "is a" Numeric

---

# Typeclass - Cleaning up

```scala
def signOfTheTimes[T: Numeric](t: T): T = {
    val ev = implicitly[Numeric[T]]
    ev.times(ev.negate(ev.abs(t)), t)
}
```

- this is a bit better.  the `T: Numeric` is a _context bound_
- the signature reads "give me any T that has a Numeric"

---

# Typeclass - Cleaning up

- We can clean this up more by introducing a "summoning implicit" on the companion object

```scala
// this is just annoying boilerplate
object Numeric {
  def apply[T](implicit numeric: Numeric[T]): Numeric[T] = numeric
}
```

and now signOfTheTimes looks like:

```scala
def signOfTheTimes[T: Numeric](t: T): T = {
  val N = Numeric[T]
  import N._
  times(negate(abs(t)), t)
}
```

- but this kind of sucks (inside out static methods vs class methods)

---

# Typeclass - Ops

It's common to introduce an `ops` on the typeclass companion:

```scala
object Numeric {
  def apply[T](implicit numeric: Numeric[T]): Numeric[T] = numeric

  object ops {
    // this is useful to add methods to a class in general
    // google extension methods
    implicit class NumericOps[T](t: T)(implicit N: Numeric[T]): {
      def +(o: T): T = N.plus(t, o)
      def *(o: T): T = N.times(t, o)
      ...
      def abs: T = N.abs(t)
      ..
    }
  }
}
```

and now our signOfTheTimes looks like:

```scala
import Numeric.ops._
def signOfTheTimes[T: Numeric](t: T): T = -(t.abs) * t
```

--- 

# Typeclass Example - Fizzbuzz 

```scala
import Monoid._  // import our typeclasses

val fizz: Int => Option[String] = x => if (x % 3 == 0) Some("fizz") else None
val buzz: Int => Option[String] = x => if (x % 5 == 0) Some("buzz") else None
val bazz: Int => Option[String] = x => if (x % 7 == 0) Some("bazz") else None

val funcs = List(fizz, buzz, bazz)

val fizzbuzzbazz = foldRight(funcs)
  
val fbbOrInt: Int => String = { i =>
  (fizzbuzzbazz(i) getOrElse i.toString) + ","
}

// map our function on a list
val strings: List[String] = (1 until 100).toList map fbbOrInt
println(foldRight(strings))
}
```

- uhhh ??? 

---

# Typeclass Monoid

```scala
trait Monoid[A] {
  // has an identity
  def empty: A
  // has an associative operation
  def combine(x: A, y: A): A
}
```

an implementation for the string type:

```scala
implicit val stringMonoid: Monoid[String] = new Monoid[String] {
  val empty = ""
  def combine(x: String, y: String): String = x + y
}
```

an implentation for Int type

```scala
implicit val intMonoid: Monoid[Int] = new Monoid[Int] {
  val empty = 0
  def combine(x: String, y: String): String = x + y
}
```

---

# Typeclass composition

Here's where typeclasses start to get interesting:

- What if I want a Monoid for Options?
- I don't want to write out a monoid for `Option[Int]`, `Option[String]`, etc.
- how do I write a monoid for Option[A]?

```scala
implicit def optionMonoid[A](implicit am: Monoid[A]): Monoid[Option[A]] = {
  val o = new Monoid[Option[A]] {
    val empty = None
    def combine(x: Option[A], y: Option[A]): Option[A] = (x, y) match {
      case (None, None)       => None
      case (Some(_), None)    => x
      case (None, Some(_))    => y
      case (Some(x), Some(y)) => Some(am.combine(x,y)) // use the A monoid to combine two A's
    }
  }
}
```

- this is wild! I don't need to explicitly define `Monoid[Option[Int]]` if I have defined `Monoid[Int]`

- very different than an interface
- we define the base elements and the composition rules
    - at compile time, the compiler applies the composition rules
    - compiler constructs the type class instances we need

---

# Typeclass comprehensibility

- FP programers will rant about "lawless type classes" online
- type class "laws" are a statement about properties that must hold for a
  typeclass to be valid
- the `Monoid` typeclass has laws:
    - the binary operation must be associative:
        - `combine(x, combine(y,z)) == combine(combine(x, y), z)`
    - the idenity must be commutative:
        - `combine(a, identity) == a == combine(identiy, a)`

- the laws help us implement systems with many type classes, since it helps us
  know what to expect.
    - this is important, as typeclasses can derive other typeclasses at compile time!

---

# Typeclasses vs interfaces (records of functions)

I was asking all over twitter, gitter, IRC about when to use a typeclass vs when to use an interface. Someone kindly wrote a
blogpost to answer this for me:

- if automatic composition will be useful then consider a typeclass
- if semantics are not well defined then don't use a typeclass
    - this is where people talk about typeclass _laws_
    - Monoid has _laws_
- if multiple instances are likely a typeclass is probably the wrong choice
- if it will make your code substantially easier to use a typeclass, it might be the right choice
    - API ergonomics

see: https://noelwelsh.com/posts/2019-06-24-type-classes-vs-record-of-functions.html

---

# Typeclasses composition++

```scala
implicit def functionMonoid[A, B](implicit bm: Monoid[B]): Monoid[A => B] =
  new Monoid[A => B] {

    def empty: A => B = _ => bm.empty

    def combine(x: A => B, y: A => B): A => B = { a =>
      bm.combine(x(a), y(a))
    }
  }
```

given a monoid for B I can give you a monoid for fns returning B

---

# Typeclasses, monoids, and fizzbuzz

```

<< scala cats example here >>
```

WTF!

---

# Monoids and Folding

```
implicit def foldRight[A](la: List[A])(implicit am: Monoid[A]): A = {
  la.foldRight(am.empty)(am.combine)
}
```

---

# fizzbuzz with typeclasses and monoids 

```scala
val fizz: Int => Option[String] = x => if(x % 3 == 0) Some("fizz") else None
val buzz: Int => Option[String] = x => if(x % 5 == 0) Some("buzz") else None
val bazz: Int => Option[String] = x => if(x % 7 == 0) Some("bazz") elze none 

val funcs: List[Int => Option[String]] = List(fizz, buzz, bazz)

// we can combine our functions. 
// this works because we can find an option monoid for strings 
// since we have an option monoid for strings, then we can find a monoid 
// for Int => Option[String] 


val fizzbuzzbazz: Int => Option[String] = fold(funcs)

// handle the None such
val fbbOrInt: Int => String = { i => 
  (fizzbuzzbazz(i).getOrElse(i.toString) + ",")
}

// map our function on a list 
val strings: List[String] = (1 until 100).toList map fbbOrInt 
println(fold(strings)) // concatenate into a single string 

```
---

# Folding and constructor replacement


Remember that a string is just defined with a cons (constructor) arguement:

```scala
List(1,2,3,4) == 1 :: 2 :: 3 :: 4 :: Nil  // this is valid scala
```

`foldRight` is just constructor replacement with some `f`, and replacing Nil with empty

```
List(1,2,3,4).foldright(empty)((a, acc) => a `f` acc)

becomes

1 `f` 2 `f` 3 `f` 4 `f` empty

e.g.

List(1,2,3,4).foldRight(0)(_ + _)

1 + (2 + (3 + (4 + 0)))
// remember symetry
1 :: 2 :: 3 :: 4 :: Nil
```

So in our example (see whiteboard)

---

# fizzbuzz with typeclasses and monoids 

- this is *wild!*
- we've done three pretty functional things: 
    - we built some small building blocks and composed them together into a real
      computation
    - we leveraged polymorphism to do very different things: look at that `fold`
      function
        - in one example, we use it to compose a function that runs multiple
            functions and figure out how to combine the results 
        - in another example, we concatenate strings 
    - we've hidden a lot of plumbing
        - you don't see loops/conditionals, etc. like you would in an imperative
          version

- whiteboard: how does this even work? foldRight? foldLeft?

---

# Typeclass derivation 

- Various ways to do automatic typeclass derivation from case classes.
    - some have compile time impacts (shapeles, kittens, others)
    - some use macros and have much less compile time impact (magnolia) 
- this is pretty common in haskell / rust to auto derive stuff
    - scala syntax is a little more wordy here but works! 

```haskell
// using haskell for the general idea

data Blah a = Ex 
   { a' :: a 
   , speed :: Int 
   } deriving (Show, Eq, Ord, Functor)
```

- auto-derives the Show, Eq, and Ord typeclasses 
- auto derives a `map` method to take a `Blah a` to a `Blah b` 
- check out shapeless / magnola / kittens for this in scala 

---

# typeclasses 

- some hairy syntax to learn 
- can be abused, important to learn when to use them because they can feel like magic 
- can be hard to debug but also powerful 
- some relationship between interface / record of functions / typeclasses

* [Type classes vs records of
  functons](https://www.inner-product.com/posts/type-classes-vs-records-of-functions/)
  by Welsh 
    - super important read actually. Shortest and read this first 
* [Essential Scala](https://underscore.io/books/essential-scala/) by Welsh and Gurnell (free)
* [Scala With Cats](https://underscore.io/books/scala-with-cats/) by Welsh and Gurnell (free)

Also [fp for mortalz with scalaz](https://leanpub.com/fpmortals) has a great chapter on typeclasses, with an ok example of modelling OAuth with a json deserialization library modelled using typeclass derivation


