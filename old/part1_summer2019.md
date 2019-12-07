---
title: Getting Func-ey
author: Mathew Fournier
patat:
  theme:
    syntaxHighlighting:
      decVal: [bold]
  images:
    backend: auto
...

---

# Big Picture 

```
      ------------------------------
      |                            |
      |      ----------------      |
      |     |                |     |
      |     |    PURE        |     |
      |     |   FUNCTIONS    |     |
      |     |________________|     |
      |                            |
      | Side-effecting functions   |
      |____________________________|
           outside world / program 
              boundary 
```

- side effecting functions (anything NOT a pure function) 
- talking to the outside world, updating DBs, etc.
- there are some tricks to wrap your head around the fact that you can still have referential transparency with side effecting functions 

---

# Pure Function

- deterministic 
- total (not partial)
- no mutation`*`
- no exception
- no null
- no reflection
- no side effect

Cannot stress this enough:

*Referential transparency means LOCAL REASONING means less surface area of code to grok*

---

# Consequences of Referential Transparency

- *All* the machinery of FP with the funny math words, and the very fact we can map some category theory to FP, comes from referential transparency. 
- We gain a ton of ability to reason about our programs
- The type signatures can act as a map to understand what's going on 

But.... what about all these *impure things* that we know we have to deal with:  

- Partiality? 
- Exceptions?
- Nondeterminism?
- Dependency injection?
- Logging? 
- Mutable state? 
- Side-effects (a program of pure functions does nothing, we need to talk to the outside world) 

---

# Effects  

- they all compute some sort of answer _with some extra stuff associated with them_ 
- the _extra stuff_ is what we call an _effect_

They all have the same Shape F[A]

```scala
type F[A] = Option[A]
type F[A] = Either[E, A] // for any fixed E 
type F[A] = List[A]
type F[A] = Reader[E, A] // for any type E
type F[A] = State[S, A]  // for any type S 
// intuition: this extends to other "effects" 
type F[A] = Future[A] 
type F[A] = Task[A]
```

- whatever distinguishes F[A] from A 
- F[A] is sometimes called a Context 
- there are many, many, many of these 
- sometimes F[A] is called "a program in F that computes a value of A" 
- sometimes F[A] is called a computation 

---

# Programming with effects

- say we've bought into effects and referential transparency and signalling intent with types
- now that everything is in a context (effect):
    - how do I operate on things inside of a context?
    - how do put things inside of a context 
    - what if I'm nested inside of a context 
        - what if both contexts are the same
        - what if both contexts aren't the same
    - what else can I do with something in a context?
    - what if I want to abstract (go generic) over my context?
- the answers to these are actually *huge* and have implications for how you put together and reason about programs

---

# How do I operate on things inside of a context?

## MAP 

- map is not _just_ a replacement for a for loop w/o explicit indexing

- we are applying something through/around some _context_ we don't care about
- we are applying something through/around some _effect_ we don't care about
- we are applying something through/around some _structure_ we don't care about 
- we are applying something through/around some _structure_ and _preserving that structure_

---

# How do I operate on things inside of a context?

- The idea behind map is called `Functor` 

```scala
trait Functor[F[_]] {
  def map[A, B](x: F[A](f: A => B): F[B]
}
```

```haskell
map :: (a -> b) -> Maybe    a -> Maybe    b 
map :: (a -> b) -> List     a -> List     b 
map :: (a -> b) -> Either e a -> Either e b
map :: (a -> b) -> F        a -> F        b
```

---

# How do I operator on things inside of a context?

## Functor 

- a functor is some sort of magic box (context, effect, etc) that we can only inspect by means of functions
- we can *never unwrap* a functor *or smoosh nested functors together*
- by calling map over some function g: 
    - you turn it into a new function that operatoes on whatever is contained in the functor 
    - this is called *lifting* 
    - since we *lift* the function g to operate at a higher level of abstraction

```scala
List(1,2,3,4).map(x => x + 1) 
```

- our anonymous function here is a function of `Int -> Int`
- we've lifted it, and now the whole thing is a function of List[Int] -> List[Int] 

---

# How do I operate on things inside of a context?

## Functor

```scala
trait Functor[F[_]] {
  // this we have to define
  def map[A, B](fa: F[A])(f: A => B): F[B]

  // this we can derive from the first, this sort of derivation 
  // is common
  def lift[A, B](f: A => B): F[A] => F[B] =
    fa => map(fa)(f)
}
```

- Functor is also a typeclass 
- We can constraint programs to require a Functor 
- these typeclasses have laws (not important here) but if you write your own Functors you can check if they are infact functors.

---


# How do I operate on things inside of a context?

## Functors compose 

- turns out two different functors compose! 
- if you find yourself inside a `List[Either[String, Future[A]]]`:
    - to do anything you need to `map(_.map(_.map(futureA => the thing I wanted to do)))`
    - this sucks

```scala
import cats.Functor
import cats.implicits._
val listOption = List(Some(1), None, Some(2))
// listOption: List[Option[Int]] = List(Some(1), None, Some(2))

// Through Functor#compose
Functor[List].compose[Option].map(listOption)(_ + 1)
// res1: List[Option[Int]] = List(Some(2), None, Some(3))
```

- if effects F and G have Functors, then so does `F[G[_]]` or `G[F[_]]`
- for nicer syntax look at the *nested* type in *cats*

---

# What if I'm nested inside of a context? 

- what if the function I'm mapping also puts something in a context?
    - I'm mapping a function over a list, but that function also returns a list 
    - I don't want `List[List[A]]`
- the dreaded Monad

---

# What I'm neseted inside of a context?
# How do I put a normal value into a context?

## Monad 

- we are mapping some function in some context, but that function also returns the same context 
    - this implies we have a *map* method (aka: every *monad* is also a *functor*) 
- we need some way to take a normal value (a pure value) and put it into a context 
    - this is called *pure* or *unit* or *point* 
    - this is just `A -> M[A]` 
    - this is just `Int -> Future[Int]` or `String => Option[String]` 
- we need a *flatten* operation 
    - this is called `join` or `flatten` 
    - this is just `M[M[A]] => M[A]`
    - this is just `Option[Option[String]] => Option[String]` 
- alternatively, we need a *sequence* operation
    - this is called `bind` or `>>=` or `flatMap` 
    - `def bind[A, B](x: M[A])(f: A => M[B]): M[B]` 

---

# What I'm neseted inside of a context?

## Monad 

- this flattening/joining or binding really meany unwrapped myself one level of the context 
- this gives rise to an ordering or a sequencing 
- the *bind* operator or the *flatten* call are dependent on the previous value 
- look at Future in th std. library: 

```scala
def flatMap[S](f: T => Future[S])(implicit executor: ExecutionContext): Future[S] = transformWith {
  case Success(s) => f(s)   // but we only get here once the future is completed
                            // how else do we know whether or not we are a success or a failure?
  case Failure(_) => this.asInstanceOf[Future[S]]
}
```

```scala
implicit val optionMonad: Monad[Option] = new Monad[Option] {
  def point[A](x: A) = Some(x) 
  def bind[A, B](x: Option[A])(f: A => Option[B]): Option[B] = x match {
    case Some(y) => f(y)  // again there is a sequencing here, I have unwrapped one layer of Option
    case None    => None
  }

```

---

# Railroad oriented programming - failure 

- the sequencing aspect of a Monad meshes with the effect of whatever you are doing 
- for option and either:
    - the effect is giving us back partial functions / exceptions 
- we can use monads to make a function that _composes_ a bunch of partial functions together
- we get what we wanted at the end OR we short-circuit with a failure 
- we get early stopping 

---

# Railroad oriented programming 

```
            ___________________________________________
           |   A single fn representing the use case   |
           |                                           |
request -> |   Validate    ->   Update    ->   Send -> | -> Success
           |      |                |             |     |
           |______|________________|_____________|_____|
                  |                |             |
                   ---------------->------------->--------> Failure 

```

- return either a success or a failure 
- each step is one step in a data flow 
- errors combined into a single failure path 

---

# Monad - imperative code w/ error handling

Contrived example ... 

```csharp
string UpdateCustomerWithErrorHandling()
{
  var request = receiveRequest();
  var isUpdated = validatedRequest(request);
  if (!isValidated) {
    return "Request is not valid" 
  }
  canonicalizeEmail(request);
  try {
    var result = db.updateDbFromRequest(request);
    if (!result) {
      return "Customer record not found"
    }
  } catch {
    return "DB error: customer record not updated"
  }

  if (!smptServer.sendEmail(request.email))) {
    log.Error "customer email not sent"
  }

  return "OK";
}
```

- 6 lines of logic, 12 lines of error handling. This sucks. 

---

# Railroad orientd programming - failure 

```scala
def parseInt(str: String): Option[Int] =
  scala.util.Try(str.toInt).toOption

def divide(a: Int, b: Int): Option[Int] =
  if(b == 0) None else Some(a / b)

def stringDivideBy(aStr: String, bStr: String): Option[Int] =
  parseInt(aStr).flatMap { aNum =>
    parseInt(bStr).flatMap { bNum =>
      divide(aNum, bNum)
    }
  }
```

- but this flatMap nesting kind of sucks..... 

---

# For comprehensions - Better Syntax (sometimes?)

- for comprehensions are *NOT FOR LOOPS*, don't think about them like for loops
- they are a poor version of haskell's *do notation* 

```haskell 
validatePerson name age = do name' <- validateName name 
                          age'     <- validateAge age 
                          return (Person name' age')

-- same as
-- return is just point / pure, aka put the thing into the context 
 
validatePersonn name age = validateName name >>= \name' -> 
                             validateage age >>= \age'  -> 
                             return (Person name' age')
```

---

# For Comprehensions 

In scala... 

```scala
for {
  x <- a
  y <- b
  z <- c
} yield e

// translates to 
a.flatMap(x => b.flatMap(y => c.map(z => e)))
```

- let's us write nicer code 
- but do notation (for comprehensions) are somewhat harmful to understanding what's going on 
- if you're for comprehension explodes with type errors, try to write it out by hand
    - you are probably lining some type in your for comphresion wrong
    - I've been doing this for years and I literally had to do this yesterday

---

# Monads

- but they are not JUST about failure 
- flatMapped futures: 
    - we are sequencing a series of asyncronous programs (that may fail)
- flatMapped Reader 
    - we are sequencing a combination of functions that all require some environment
    - this is dependency injection 
- flatMapped Writer
    - we are sequencing a combination of functions that all do some work and emit some extra information 
    - sort of like logging 
- there is a monad for resource management ..
- there is a Par monad for sequencing work in parallel
- there is a Probability effect, where the monadic sequencing is integration 
    - woah...

- they are a huge idea and we just see their most basic form in most of our code 
- this is fine becuase the plumbing of the sequencing is handy. 

---

# Maps and fancy maps

The story so far: 

- I have some function I want to apply through some structure (without affecting that structure)
- I have some function I want to apply through some structuree 
    - but that function also returns some structure 
    - I need to destroy some structure to flattne my structure
    - this destruction causes dependent sequencing 
    - this sequencing means that in a sequence of operations, each operation depends on the one before this


```haskell
map     ::   (a ->   b) -> f a -> f b      .. FUNCTOR 
flatMap ::   (a -> f b) -> f a -> f b      .. MONAD
(>>=) 
```

---

# Maps and fancy maps

- but what if I have a series of effectful computations that DON'T depend on each other? 
- what if I have independent rather than dependent computations 


```haskell
map     ::   (a ->   b) -> f a -> f b      .. FUNCTOR 
<*>     :: f (a ->   b) -> f a -> f b      .. APPLICATIVE
flatMap ::   (a -> f b) -> f a -> f b      .. MONAD
(>>=) 
```

- Applicative: what if the function I'm applying is *also stuck in a context*

---

# Applicative 

- this doesn't often show up directly, but happens behind the scenes in many of the libraries we use, e.g. Validation 
- intuition: we are applying some function _through_ some context 
    - except the function is also _stuck in some context_ 
    
```haskell
<*>  ::f (a -> b) -> f a -> f b 
```

In scala this is called `ap`: 

```scala
trait Applicative[F[_]] extends Functor[F] {
  def ap[A, B](ff: F[A => B])(fa: F[A]): F[B]
  def pure[A](a: A): F[A]
  def map[A, B](fa: F[A])(f: A => B): F[B] = ap(pure(f))(fa)
}
```

---

# What if my effectful computations don't depend on each other?

This is weird to think about in Scala (compared to Haskell), it's easier to
think of it in terms of `product`

```scala
trait Applicative[F[_]] extends Functor[F] {
  def product[A, B](fa: F[A], fb: F[B]): F[(A, B)]
  def pure[A](a: A): F[A]
}
```
- Given a Future[Int], and a Future[String], I can run them both and give you a Future[(Int, String)]
- intuition for product: composing multiple _independent_ effects 
- intuition: we need this to do our friend _sequence_ where we turn
`List[Future1, Future2, Future3] into Future[List[1,2,3]]`
- intuition: parallel processing 

---

# Applicative - why do we care?

- Validation type for collecting errors (switch to that presentation)

---

# Applicative - why do we care? 

- Suppose I want to aggregate data from 2 remote services and serve responses as
  fast as I can:
  
```scala
def loadUser: Future[User]
def loadData: Future[Data]
case class Payload(user: User, data: Data)
```

```scala
// sequentially
for {
  user <- loadUser
  data <- loadData_
} yield PayLoad(user, data) 
```

---

# Applicative - why do we care?

- Suppose I want to aggregate data from 2 remote services and serve responses as
  fast as I can:
  
```scala
// in parallel since async tasks are triggered before being chained 
// turns out Future is not referentially transparent... 
val userF = loadUser 
val dataF = loadData 
for {
  user <- userF
  data <- dataF
} yield Payload(user, data)
```

---

# Applicative - why do we care?

- Suppose I want to aggregate data from 2 remote services and serve responses as
  fast as I can:
 
```scala
// in parallel since you can zip futures 

for ((user, data) <- loadUser zip loadData) yield Payload(user,data)

// but zipping more than two futures sucks
```

```scala
// applcative also solves the job 

(userF, dataF).mapN {
 case(u, d) => Payload(u, d)
}

// and much easier to use when you have more than two 

```

---

# Applicative - why do we care 

- sequence for inverting containers 
- traverse for applying a function AND inverting containers 

---

# Applicative - why do we care 

- much like we have a bunch of effects: 
    - type F[A] = Option[A]
    - type F[A] = Either[E, A] // for any fixed E 
    - type F[A] = List[A]
    - type F[A] = Reader[E, A] // for any type E
    - type F[A] = Writer[W, A] // for any type W 
    - type F[A] = State[S, A]  // for any type S 
- we may have nested types, e.g. List[Future[A]] 
    - aka we may have F[G[A]] 
- we want to flip these inside out, to get G[F[A]] 
- we use traverse for this 

- see examples in `polling-instagram`

---

# Refresh: Haaaalp, I'm in a context / effect

- say we've bought into effects and referential transparency and signaling intent with types
- now that everything is in a context (effect):
    - how do I operator on things inside of a context?
    - how do put things inside of a context (generically)?  
    - what if I'm nested inside of a context?
        - what if both contexts are the same?
        - what if both contexts aren't the same?
    - what else can I do to something in a context?

- the answers to these are actually *huge* and have implications for how you put together and reason about programs

---

# What else can I do to something in a context?

- I can always destory my context to get some value `A`
    - I can do this with a function: `def blah(x: List[A]): B` 
- I can convert one context into another 
    - folding! 

---

# What else can I do to something in a context?

## Fold

- fold is the essence of the iterator pattern 
- it *can be* structure destroying 
- it encodes the idea of recursion 
- it *can* encode the idea of a *while loop* 
- fold has both a *mapping* and an *accumulating* aspect 
- not every Context is *foldable*
    - you guessed it, foldable is another typeclass
    - traversable is another typeclass (and requires applicative)
- it *can be* slow, sometimes a while loop + an array is the better choice
---

# What else can I do to something in a context?

## Fold 

- fold is a workhorse 
    - when combined with Option/Either, you can accumulate results with early exit 
    - you can shrink a context down to a single value (summing a list)
    - you can keep the context but modify it some way (shrink a list, change the structure of a tree)
    - you can implement map via fold (at least for Lists)
    - you can fold functions together (what the what)

---

# FoldLeft vs FoldRight 

- foldRight is not *stack safe* 
- foldRight is just constructor replacement 
- foldLeft is a _for loop_ operating on a stack
- there is a difference  with lazy vs. eager folds too (applicable to streams...) 

- If your operation is *associative* then it doesn't matter which one you use 
- If your operation is *not associative* you may have to correct some behaviors 

(board examples constructor replacement) 

---

# What if the thing inside my context has known properties? 

- what the...

```scala
import cats.implicits._

val v1: Option[Int] = Some(1) 
val v2: Option[Int] = Some(10)
val v3: Option[Int] = None

//monoidal:
val program1: Option[Int] = v1 |+| v2 |+| v3  // Some(11) 

//monadic:
val program2: Option[Int] = for {
  i   <- v1 
  ii  <- v2
  iii <- v3
} yield i + ii + iii                         // None 

//applicative:
val program3: Option[Int] = (v1, v2, v3).mapN(_ + _ + _) // None 
```

- linear algebra analogy here. 

---

# What if I'm nested in a context 

## and those contexts aren't the same
## And those contexts are monads 

- now that I've gotten a taste for contexts, what do I do when I'm stuck in `F[G[_]]` ?
    - common for us: `Future[Either[Error, Result]]`

```scala
def lookupUser(id): Future[Either[Error, String]] = { ... }
def lookupUserName(id: Long): Future[Either[Error, String]] =
  for {
    maybeUser <- lookupUser(id)
  } yield {
    for { user <- MaybeUser } yield user.name
  }

```

- this sucks. 

---

# What if I'm nested in a context 

## and those contexts aren't the same
## and those contexts are monads 

- Well, if I was two Functors inside of each other I had a way around this 
- turns out there is no way two generically combine two monads so that `flatMap` works

BUT! 

- if you know what _one_ of the contexts is, you can. 

```scala
                   outer monad
                     |
                     |    right of either
                     |      |
case class EitherT[F[_], E, A](stack: F[Either[E, A]]) {
                         |
                    left of either


```
---

# What if I'm nested in a context 

## and those contexts aren't the same
## and those contexts are monads 
## and I know what one of those contexts are

```scala
case class EitherT[F[_], E, A](stack: F[Either[E, A]]) { ... }
// this is similar to Future[Either[E, A]]
```

```scala
def lookupUser(id): Future[Either[Error, String]] = { ... }
def lookupUserName(id: Long): EitherT[Future, Error, String]] =
  for {
    user <- EitherT(lookupUser(id))
  } yield user.name
```

- the monad transformer knows about the context in-between
- it knows how to jump straight to the right of the either
- we get out (unwrap) a monad transformer by using EitherT.value 
- extremely useful

---

# What if I want to abstract over the context?

## Abstracting over `F[_]` 

```scala
class AirlineService[F[_] : Functor](airlineRepo: AirlineRepository[F]) {

  def baggagePolicy(airlineName: AirlineName): F[Either[ValidationError, Airline]] =
    airlineRepo.findAirline(airlineName).map { airline =>
      airline.toRight[ValidationError](AirlineNotFound(airlineName))
    }
```

- we are definitely not in OOP land anymore here 

---

# Higher Kinded Types 

- we are used to higher order functions 
- there is also something called Kinds, which _types_ have 
- intuition: the type of types / type constructions 

the _kind_ of List is `* -> *` 
the _kind_ of List[Int] is `*`  (where * = Int) 

In order to get a List[Int] we need to supply a type arguement to List. This is
going on behind the scenes in the compiler:

```scala
def foo[F[_]]: F[Int]
        ^^^^     ^^^
         |        + supply a type argument to F[_]  
         |
         + the type constructor F[_] 

```

```scala
scala> :k List 
List's kind of F[+A]
scala> k: List[Int]
List[Int]'s kind is A
```
- List[+A] takes a type parameter (A)
- this isn't a valid type but is a type constructor waiting for an A 
- by filling it with Int, we get List[Int] which is a concrete type

---

# Kinds 

* Kinds are like types for types.  
* We care about something called _Higher Kinded Types_ 
* This is a fancy way of saying _Type Constructor_ 

* There is a weird syntax when talking about this.  
    - Kinds describe the numbner of "holes" in a type
    - The Kind of an ordinary Type like `Int` or `Char` is `*` (zero holes) 
    - The Kind of a unary type constructor such as `Maybe` is `* -> *` (one hole)
    - The Kind of a binary type constructor such as `Either` is `* -> * ->*` (two holes) 

---

# Higher Kinded Types and Type Constructors 

* So we distinguish regular types (no holes) and "type constructors" which are Higher Kinded Types
* Be careful not to confuse type constructors w/ generic types 

```scala
List    // type constructor, takes one parameter 
List[A] // type, produced using a type parameter 
```

* There is a close anology w/ functions and values

```scala
math.abs // function, takes one parameter 
math.abs(x) // value, produced using a value parameter 
```

* In scala we declare a type constructor using underscore e.g `trait blah[F[_]] { .. }`
* Why would you do such a thing? It lets you get _really generic_

---

# Abstracting over my context

## Demo

- to intelliJ

---

```scala
scala> :k List 
List's kind of F[+A]
scala> k: List[Int]
List[Int]'s kind is A
```
- List[+A] takes a type parameter (A)
- this isn't a valid type but is a type constructor waiting for an A 
- by filling it with Int, we get List[Int] which is a concrete type

---


# Resources

## BOOKS
In this order: 

- [Essential Scala](https://books.underscore.io/essential-scala/essential-scala.html)
- the first 3-4 chapters of Functional And Reactive Domain Modeling 
- [Scala With Cats](https://underscore.io/books/scala-with-cats/)

## Workshops 

- [fp foundation course](https://github.com/fp-tower/foundation) is great
---

# Resources 
## Talks 

- [how to build a functional API by the guy who made the Fp foundation
  course](https://www.youtube.com/watch?v=__zuECMFCRc)
- [constraints liberate, liberties
  constraint](https://www.youtube.com/watch?v=GqmsQeSzMdw) - up the 36 minute
  mark, after that it's not useful to the beginner 

More of an advanced talk, but a very useful mindset to get into eventually: 

- [programming with effects](https://www.youtube.com/watch?v=po3wmq4S15A) up to about the 27th minute mark

Another advanced talk that is fascinating in terms of the functional mindset, but much more functional than we do here 

- [building a functional SQL library](https://d35r1ltz73o8oz.cloudfront.net/videos/capture/Laovsfq9cSNzieisbP953N.mp4)

---

# Resources
## Repositories 

- These repos are QUITE functional (more-so than anything we have running around
  at work) but are worth looking at. 
- Notice how contained everything is. 
    - for comprehensions are small/simple/easy to read 
    - no huge nesting of if statements, pattern matches, anonymous functions, etc. 
- I think they are good examples of what to shoot for in your code in terms of class size/responsibility, function size, for comprehensions

- [backpacker core](https://github.com/SmartBackpacker/core)
- [skunk](https://github.com/tpolecat/skunk)
- [scala pet store](https://github.com/pauljamescleary/scala-pet-store)

# Advanced Resources

- everyone loves haskell 
    - Haskellbook or get programming in haskell 
    - parallel and concurrent programming in haskell
- fp for mere mortals
- the sky is limit...
