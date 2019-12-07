# Programming with EFfects Addendum

- one consistent example showing a bunch of effects
- some of these effects short circuit
    - give us back exceptions (option, either)
- some represent something happening async elsewhere (possible on another thread) 
    - e.g. waiting for something on a network
    - e.g. Future/Task
    - note: these _also_ have some idea of short circuiting with failures 
- some have nothing at all to do with failures and have no short circuiting 
    - e.g. Reader, Writer, State 


The example we will use is some generic function that takes in three of the same context/effect/whatever, monadically (flatMap!) combines them in order to create a User, and returns the user in that same context/effect/whatever.  We will run this same `doThing` over and over by supplying different effects for `F`: Option, Either, Future, monad transformers, Reader, Writer, IO and we will observe the different behavior we get out of the same code but different effect.

```scala
case class User(name: String, id: Long, age: Double)

object Thing {

    def doThing[F[_]: Monad](a: F[String], b: F[Long], c: F[Double]): F[User] = for {
        aa <- a
        bb <- b
        cc <- c
    } yield User(aa, bb, cc)
}
```

- We have some series of computations and we are trying to generate a `User`.
- remember that the above `for comprehension` *IS NOT A FOR LOOP*
- it just turns into this: 

```scala
def doThingS[F[_]: Monad](a: F[String], b: F[Long], c: F[Double]): F[User] = 
    a.flatMap(aa => b.flatMap(bb => c.map(cc => User(aa, bb, cc))))
```

- Here we are talking about monads and the sequencing of monadic effects.
- the `F[_]: Monad` says give me ANY *box* / *container* / *effect* that implements the *typeclass* *Monad* and I'll give you back a User inside that same *box* (maybe). 

Remember that *Monad* is just: 

```scala
trait Monad[F[_]] {
  // some way to put a value into a monad
  def pure[A](value: A): F[A]

  // some way to collapse the nested monad down
  // e.g. I am mapping some fn over my context but that fn also returns the context
  def flatMap[A, B](value: F[A])(func: A => F[B]): F[B]
}
```

### so in the following we change the effect (change the F) and run everything through the above doThing function, for surprising results that get to the heart of function composition.

## Option

- gives us back total functions when something can go wrong

```scala
// option 
object ValidOptionThing {
    val oName: Option[String] = Some("mat")
    val oId: Option[Long] = Some(17828382L)
    val oAge: Option[Double] = Some(1.3)
}

object InvalidOptionThing {
    val oName: Option[String] = None
    val oId: Option[Long] = ValidOptionThing.oId
    val oAge: Option[Double] = ValidOptionThing.oAge
}

// Some(User(mat, 1782382, 1.3))
object validOThing extends App {
    val res = Thing.doThingS(ValidOptionThing.oName, ValidOptionThing.oId, ValidOptionThing.oAge)
    println(s"res: $res")
}
// None 
object invalidOThing extends App {
    val res = Thing.doThing(InvalidOptionThing.oName, InvalidOptionThing.oId, InvalidOptionThing.oAge)
    println("we short circuit since aa is none, b and c never evaluated")
    println(s"res: $res")
}
```

- When we run doThing with 3 valid options, we get back Some(User)
- When we run doTHing with 2 valid options and one None, we get back None 

The compiler turns out generic function `doThing` into something like the following at compile time: 

```scala

    def doThing(a: Option[String], b: Option[Long], c: Option[Double]): Option[User] = for {
        aa <- a
        bb <- b
        cc <- c
    } yield User(aa, bb, cc)
```

I will skip pointing out this translation step in the remainder of the examples.

## Either 

- gives us back total functions when something can go wrong and let's us put in what went wrong
- gives us back exceptions
- in this example, our left is Throwable, which is totally allowed! 

```scala
// either

object ValidEitherThing {
    type VEThing[A] = Either[Throwable, A]
    val oName: VEThing[String] = "mat".asRight
    val oId: VEThing[Long] = 17828382L.asRight
    val oAge: VEThing[Double] = 1.3.asRight
}

object InvalidEitherThing {
    type VEThing[A] = Either[Throwable, A]
    val oName: VEThing[String] = new java.lang.NoSuchFieldError("nope").asLeft
    val oId: VEThing[Long] = 17828382L.asRight
    val oAge: VEThing[Double] = 1.3.asRight
}

 // Right(User(mat,17828382,1.3))
object validEThing extends App {
    val res = Thing.doThing(ValidEitherThing.oName, ValidEitherThing.oId, ValidEitherThing.oAge)
    println(s"res: $res")
}

// Left(NoSuchFieldError("nope"))
object invalidEThing extends App {
    val res = Thing.doThing(InvalidEitherThing.oName, InvalidEitherThing.oId, InvalidEitherThing.oAge)
    println("we short circuit since aa is none, b and c never evaluated")
    println(s"res: $res")
}
```

- When we run doThing with 3 right values, we get back `Right(User)`
- when we run doTHing with 2 right values and a left, we short circuit and get Left(what went wrong) 

## Future 

- futures are not referentially transparent, but do have a monad
- prefer to use Task

```scala
// future

object ValidFutureThing {
    val name: Future[String] = Future.successful("mat")
    val id: Future[Long] = Future.successful(17828382L)
    val age: Future[Double] = Future.successful(1.3)
}

object InvalidFutureThing { object applicativeInvalidTThing extends App {
    val name: Future[String] = Future.failed(new TimeoutException("nope"))
    val id: Future[Long] = Future.successful(17828382L)
    val age: Future[Double] = Future.successful(1.3)
}

object validFThing extends App {
    import ExecutionContext.Implicits.global
    val res = Thing.doThing(ValidFutureThing.name, ValidFutureThing.id, ValidFutureThing.age)
    val r = Await.result(res, Duration.Inf)
    println(s"res: $r")
}

object invalidFThing extends App {
    import ExecutionContext.Implicits.global
    val res = Thing.doThing(InvalidFutureThing.name, InvalidFutureThing.id, InvalidFutureThing.age)
    val r = Await.result(res, Duration.Inf)
    println(s"res: $r")
}
```

- When we run doThing with 3 successful Futures, we get back `User(mat,17828382,1.3))`
- When we run doThing with something going wrong --say one of those futures times out, we get back `[error] java.util.concurrent.TimeoutException: nope` since we are not handling the exception case
    - in this case, the futures for `bb` and `cc` were never run. If those contacted FB/Insta, that would never have happened. Monads are sequential.  We shortcircuit out on aa since aa failed. 


## Task 

- a referentially transparent future and a lovely library in general (Monix)
- looks similar to the above example 

```scala
// Task 
object ValidTaskThing {
    val name: Task[String] = Task.now("mat")
    val id: Task[Long] = Task.now(17828382L)
    val age: Task[Double] = Task.now(1.3)
}

object InvalidTaskThing { 
    val name: Task[String] = Task.raiseError(new TimeoutException("nope"))
    val id: Task[Long] = Task.now(17828382L)
    val age: Task[Double] = Task.now(1.3)
}

object validTThing extends App {
    val res = Thing.doThing(ValidTaskThing.name, ValidTaskThing.id, ValidTaskThing.age)
    val task = res.runAsync
    val r = Await.result(task, Duration.Inf)
    println(s"res: $r")
}

object invalidTThing extends App {
    val res = Thing.doThing(InvalidTaskThing.name, InvalidTaskThing.id, InvalidTaskThing.age)
    val task = res.runAsync
    val r = Await.result(task, Duration.Inf)
    println(s"res: $r")
}
```

this behaves the same as the Future case.

- When we run doThing with 3 successful Tasks, we get back `User(mat,17828382,1.3))`
- When we run doThing with something going wrong --say one of those Tasks times out, we get back `[error] java.util.concurrent.TimeoutException: nope` since we are not handling the exception case
    - in this case, the futures for `bb` and `cc` were never run. If those contacted FB/Insta, that would never have happened. Monads are sequential.  We shortcircuit out on aa since aa failed. 


## Aside - Applicative 

- what if aa, bb, cc were not dependent to each other in the above example? 
- infact they have nothing to do with each other in this example
- we can use `Applicative` rather than `Monadic` behavior here. 
    - run a sequence of _independent_ computations and combine the result 

- sadly, no more for syntax
    - haskell ripped for syntax from haskell's do notation
    - haskell has an `ado` syntax for applicative we did not steal :( 

- we can write something close to what we originally had though, replacing the `Monad` constraint with an `Applicative` constraint: 

```scala
object ApplicativeThing {
    def doThingA[F[_]: Applicative](a: F[String], b: F[Long], c: F[Double]): F[User] =
     (a, b, c).mapN {
         case (aa, bb, cc) => User(aa, bb, cc)
     }
}
```

If we run something like the failing task example we get much the same answer as before: 

```scala
object applicativeInvalidTThing extends App {
    val res = ApplicativeThing.doThingA(InvalidTaskThing.name, InvalidTaskThing.id, InvalidTaskThing.age)
    val task = res.runAsync
    val r = Await.result(task, Duration.Inf)
    println(s"res: $r")
}
```

- we blow up with that exception from the timed out task 
- the big difference here is *NO SHORT CIRCUITING* 
- Tasks B and C were still run! 
    - if these were requests to FB, then the first request failed, but the next two requests still happened
    - this is very different than the behavior of Task with Monad
    - Task with Applicative is about "independent" computations 

## Aside - Applicative - Validation 

- what if I'm validating a webform or something and I want to know everything that goes wrong 
    - NOT just the first thing that goes wrong (monadic behavior)

- Validation is like an Either, except it accumulates errors.
- `ValidationNel[String, A]` just means you are a right of A OR a left of (nonEmptyList of strings)

```scala
object ValidValidationThing {
    type NelThing[A] = ValidatedNel[String, A]
    val oName: NelThing[String] = "mat".validNel
    val oId: NelThing[Long] = 17828382L.validNel
    val oAge: NelThing[Double] = 1.3.validNel
}

object InvalidValidationThing {
    type NelThing[A] = ValidatedNel[String, A]
    val oName: NelThing[String] = "username invalid".invalidNel
    val oId: NelThing[Long] = 17828382L.validNel
    val oAge: NelThing[Double] = "age invalid too".invalidNel
}
```

if you try to use this monadically, it is a compiler error, since ValidationNel has no Monad 

```scala
object validVThing extends App {
    val res = Thing.doThing(ValidValidationThing.oName, ValidValidationThing.oId, ValidValidationThing.oAge)
    println(s"res: $res")
}

// :( compiler error)
```

but ValidationNel has an `Applicative` behavior: 

```scala
object validVThing extends App {
    val res = ApplicativeThing.doThingA(ValidValidationThing.oName, ValidValidationThing.oId, ValidValidationThing.oAge)
    println(s"res: $res")
}

object invalidVThing extends App {
    val res = ApplicativeThing.doThingA(InvalidValidationThing.oName, InvalidValidationThing.oId, InvalidValidationThing.oAge)
    println("we short circuit since aa is none, b and c never evaluated")
    println(s"res: $res")
}
```

- when we have 3 valid elements for a, b, c (e.g. 3 rights) we get: `Valid(User(mat,17828382,1.3))`
- but if we have 2 of the three invalid things: 
    - ` Invalid(NonEmptyList(username invalid, age invalid too))` 
    - we get ALL our errors with *no short circuiting* 
    - if we had used Either, we would have only got that the username was invalid 


## Aside - Nested Task/Either 

- what if we end up with a `Task[EIther[Throwable, User]]`
- `Thing.doThing` doesn't work! it's a compiler error since the constructor to User doesn't take in `Either[Throwable, String]` for `a` or `Either[Throwable, Long]` for `b` which is what is happening in the for comprehension

- we have to write something with two for loops like this: 

```scala
object nestedTaskEither {
    type NestedTask[A] = Task[Either[Throwable, A]]

      def doThingNested(
        a: NestedTask[String], 
        b: NestedTask[Long], 
        c: NestedTask[Double]): NestedTask[User] = for {
        aa <- a
        bb <- b
        cc <- c
    } yield for {
            aaa <- aa
            bbb <- bb 
            ccc <- cc
    } yield User(aaa, bbb, ccc)
}

```

This *sucks* but it does work 

```scala
object Nested extends App {

    import nestedTaskEither._
    val teName: NestedTask[String] = Task.now("mat".asRight)
    val teId: NestedTask[Long] = Task.now(17828382L.asRight)
    val teAge: NestedTask[Double] = Task.now(1.3.asRight)


    val res = doThingNested(teName, teId, teAge)
    val task = res.runAsync
    val r = Await.result(task, Duration.Inf)
    println(s"res: $r")
}
```

- how do we make this work with the original `doThing`? 
- how do I not have to do this annoying double unpacking of an effect inside an effect
- we can use a [monad transformer](https://typelevel.org/cats/datatypes/eithert.html) to get us back to where we want though.  This one _knows_ about the future in between

```scala
object monadTransformer extends App {

    type TE[A] = EitherT[Task, Throwable, A]

    val mtName: TE[String] = EitherT(Task.now("mat".asRight))
    val mtId: TE[Long] = EitherT(Task.now(17828382L.asRight))
    val mtAge: TE[Double] = EitherT(Task.now(1.3.asRight))
    
    // call our original method at the top of the file that only 
    // has the single for comprehension 
    val res = Thing.doThing(mtName, mtId, mtAge)

    val task = res.value.runAsync
    val r = Await.result(task, Duration.Inf)
    println(s"res: $r")

}
```

- aaaaaaaaand it all works again and we get back `Right(User(mat,17828382,1.3))`

## Back to Monads and effects 

The previous examples were all more about failure.  Which is cool.  This is super useful for us.  But there is way more to Monads than that.

## List 

- this one doens't really make that much sense in our example
- List imbues the effect of "multiple results" 
- we get _all possible results_ in this case 

```scala
object listThing extends App {

    val userNames = List("mat", "steve", "jim")
    val ids = List(1L)
    val ages = List(1.3, 2.7, 99.9)

    val res = Thing.doThing(userNames, ids, ages)
    println(s"res: $res")
}
```

In previous examples, we got just one result. In this case we get all combinations of our inputs. In this particular example, it's stupid but I have used variations of this in production code for useful functions. 

```
User(mat,1,1.3)
User(mat,1,2.7)
User(mat,1,99.9)
User(steve,1,1.3)
User(steve,1,2.7)
User(steve,1,99.9)
User(jim,1,1.3)
User(jim,1,2.7)
User(jim,1,99.9)
```

## Reader 

- intuition: dependency injection
    - there are many ways to do functional DI, this is just one 
- but we can just use constructors for dependency injection 
    - very true, often the right idea! 
    - see pros and cons [here](https://stackoverflow.com/a/30880856/3230177)
    - [also see pros and cons here](https://stackoverflow.com/a/22271372/3230177)


- Reader (and it's Monad) let us sequence operations that depend on some input 

```scala
object readerThing extends App {

    // some config or whatever class we are injecting 
    case class Injected(version: String, idShift: Long, ageShift: Double) 

    // two different versions of the thing we want to inject
    val injected = Injected("3.2", 200000L, 37.2)
    val someOtherInjected = Injected("9.9", 382973238L, 99.9)


    // the inputs to doThing 
    type Config[A] = Reader[Injected, A] 
    val rName: Config[String] = Reader(in => s"${in.version}:mat")  
    val rId: Config[Long] = Reader(in => 17828382L + in.idShift)
    val rAge: Config[Double] = Reader(in => in.ageShift + 1.3)


    // this is the "program" returned from running doTHing 
    // it hasn't done anything yet, it's really more like
    // a function that is waiting for an input before the result
    // e.g. program is a function of Injected => User 
    val program = Thing.doThing(rName, rId, rAge)

    // this just gives us the composed "program" waiting for an input
    // we need to supply the input 

    val runRes = progam.run(injected)
    println(s"res: $runRes")

    // run it with some other config injected
    val runRes2 = program.run(someOtherInjected)
    println(s"res: $runRes2")
}
```

- now this is *super interesting* 
- notice how making the program `Thing.doThing(rName, rId, rAge) hasn't done anything
    - it just gives us back a Reader[Injected, User] but not the actual user 
- we need to supply it with the config we want, for it to work
- we can _re-use the same program_ by applying a different config and getting a different user 

- runRes uses injected and gives us back:
    - `User(3.2:mat,18028382,38.5)`
- RunRes2 injects something else entirely and gives us back: 
    - `User(9.9:mat,400801620,101.2)`


## Writer

- we want to run some computation and while we are doing that we want to annotate some computations
- feels like tracing/logging (but doesn't have to be) 
- tldr; don't use a writer for logger, but they do come in useful from time to time

```scala
object writerThing extends App { 

    case class Computation(notes: String, money: Int)

    type Trace[A] = Writer[List[Computation], A]

    val wtName: Trace[String] = for {
        a <- "mat".pure[Trace]
        _ <- List(Computation("fetched user", 100)).tell
    } yield a

    val wtId: Trace[Long] = for {
        a <- 17827382L.pure[Trace]
        _ <- List(Computation("fetched id", 1000)).tell
    } yield a

    val wtAge: Trace[Double] = for {
        a <- 1.3.pure[Trace]
        _ <- List(Computation("fetched age", 10000)).tell
    } yield a

    val program = Thing.doThing(wtName, wtId, wtAge)
    val (notes, user) = program.run
    println(s"trace: $notes \n ----------\n")
    println(s"user: $user")

}
```
- running this through our generic `DoThing` will create a `User` just like all the other examples
- it also annotates with the extra computation information! 

results: 

```
notes: List(Computation(fetched user,100), Computation(fetched id,1000), Computation(fetched age,10000)) 
 ----------

user: User(mat,17827382,1.3)

```

# The Big Picture !!!!!
Other effects that don't have anything to do with failure behave the same as Reader/Writer above. The program is a Reader composed of other readers.  Or a program of writers composed with other writers. 

- we could KEEP ON COMPOSING READERS! and only at the end of the world (edge of program) do we supply the initial config value. 

- this style of DI does not play nice with runtime object dependency graphs FYI. 

- this program composition is a way different way to think about putting together programs than FP. 


## Bonus Weirndess -  IO 

- what if we encoded a program that printed to the screen instead? 
- and we were composing functions that did some sort of IO (say screen printing) 
- IO is a huge type - it can do anything, but the important thing to remember is it usually doing something out in the world 

```scala

object ioThing extends App {

    val ioUser: IO[String] = IO {
        println("side effect: username: Mat")
        "mat"
    }
    val ioId: IO[Long] = IO { 
        println("side effect id: 17828382L")
        17827372L
    }
    val ioAge: IO[Double] = IO { 
        println("side effect: age 1.3")
        1.3
    }

    val program = Thing.doThing(ioUser, ioId, ioAge)

    val res = program.unsafeRunSync()
    println(s"\nres: $res")
    
}

```

this prints out

```
side effect: username: Mat
side effect id: 17828382L
side effect: age 1.3

res: User(mat,17827372,1.3)
```

