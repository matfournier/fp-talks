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

# Goals for today 

- understand where some of the movitation for doing FP comes from 
- understand some trade-offs 
- be inspired! be curious! it's interesting! it's fun! 
- don't panic!

---

# Outline 

- Motivation
    - What is FP and why? 
    - Data vs Objects 
    - Referential transparency 
    - Even more constraints 
    - Trade-offs
- Where to go from here

---

# OOP 

This is the background that most folks are familiar with.  

- some Java/C++ from school 
- go from a networking class 
- JS and maybe TS for the frontend folks 

So what are some features of OOP languages?

---

# OOP

- everything is an object (well, in smalltalk anyway, in Java _almost_ everything)
- internals should be hidden awy 
- "What _things_ are present in my system, what messages do they send, how do they represent internal info"
- inheritance?
- dependency injection? DI frameworks?
- SOLID, etc. 
- mutation, mutation, mutation 
    - what is a constructor but something that mutates an uninitialized object into it's initial state? 

---

# OOP Style 

Sandi Metz gives good presentations on this:

- OO affordances expect you to be:
    - anthropomporhic 
    - polymorphic
    - loosely-coupled
    - role-playing 
    - factory creating objects 
    - that communicate by sending messages 

---

# FP

What describes FP for you?

---

# FP

- What data do I have and how do I transform it? 
- First class functions (java, python, JS) w/ nice anonymous function syntax 
- Immutability? (but even haskell has mutable variables!)
- Higher-ordered functions? (functions that return functions) 
- Types? (but JS is rather functional and has no types, see also: Lisps, Racket, Erlang, Exlixir) 
- Type classes? (but Rust has these and is definitely not a functional programming language)
- Higher-kinded types? (this is where F#, etc. start to drop off since they don't have them) 
- *Referential Transparency* (more on this later)

Will restrict talk to "statically typed functional programming with higher-kinded types e.g. Scala, haskell

key intuitions:

- the flow of _data_ through a system 
- programs as values, programs as data 
- compile time type safety over runtime reflection 

---

# FP - A SPECTRUM

```
                      FP ----> 
*************************************************************--> weirder stuff (Idris, Coq)
|       |      |  |       |         |       |       |
C,      |      | Python?  Kotlin    F#,    SCALA,  Haskell
ASM,   Rust?  Java?       |         JS,        OCaML?
       Go?               C++?      Lisp?,
                                    Racket?Scheme?
```

---

# Goals and Good Design

At the end of the day our job is to: 

* build things that provide business value to our employer 
* minimize the cost of change (so we can build more things) 
  * we want things to evolve easily since business needs change rapidly 
* minimize the cost of fixing bugs (so we can spend more time building things to provide business value) 
  * we want to understand old code quickly
  * we want other people to wrap their heads around our code quickly 
* not blow up at runtime 

---

# Good Design

- Many things in one are also good in the other: 
    - small scope
    - dependency injection
    - loose coupling
    - private/safe constructors
    - many more 

You will write terrible code using both approaches. 

---

# To type systems!

---

# A rich expressive type system 

Scala has one! they help you: 

- communicate clearly with your team 
    - now and in the future 
- analyze code 

- they are a type of documentation
    - a weak type, but in the absence of actual documentation they are gold 

Scala affords us a rich type system, and it's up to us to actually use it. 

---

# A good type system

A good type system lets you: 

- describe *stuff*
- derscribe the *relationships* between stuff 
- describe the *context* of stuff

This is our entire job as developers.

A big theme in FP is: 

- constrain the stuff 
- make impossible states unrepresentatble in the relationships between the stuff 
- constrain the context 

---

# Describe the stuff ...

---

# DATA AND OBJECTS 

- programming language give us tools to represent things. 
    - Sometimes things are values (the integer 10), same as any other 10. 
    - Sometimes things have identity, this 10 is different than 10 (java hascode/equality folks screaming here)
    - Sometimes things become other things (that 10 becomes a 20) 
    - Sometimes we want internals to be hidden away, otheres not 
    - Sometimes we care about how things get used or how they get extended
    
- OOP goes down the Object path, FP goes down the data path 

---

# THE EXPRESSION PROBLEM

Classes:

- It is _very cheap_ to add a new kind of thing
    - just add a new subclass, and as needed you define specialized method
- It is _very expensive_ to add a new operation on things
    - you have to add a new method declaration to the superclass
    - potentially add a method definition to every existing subclass. 
    - in practice, the burden varies depending on the method.

---

# THE EXPRESSION PROBLEM

Data:

- It is _very cheap_ to add a new operation on things: 
    - you just define a new function. 
    - All the old functions on those things continue to work unchanged.
- It is _very expensive_ to add a new kind of thing: 
    - you have to add a new constructor an existing data type
    - you have to edit every function which uses that type.

---

# JAVA Data 

- Java doesn't really want you to make data 
    - it doesn't exist on its own 
    - there is a lot of noise around it 
    - it's not concise 
- Objects are about coupling shared (usually mutable) state and behavior 

```java
public class Order {
  private String id;
  public String getId() {
    return this.id;
  }
  public void setId (final String id) {
    this.id = id; 
  }

  private int value; 
  ...
}
```

---

# JSON 

- JSON isn't a data description
- JSON is an *infinite number of examples* of what data *may* look like

```javascript
{
  "id": "ORD0001",
  "value": 315.0,
  "payment_method": "Visa"
}
```

---

# A good type system for data 

A good language would have some way to concisely, human-readably, describe data 

- Scala comes close! 
- Scala has a concise syntax for
    - higher order functions 
    - generics 
    - higher kinded types 
- Scala supports ADTs 

---

# Product Types 

- Every language has what are called Product types:

```c
struct Order {
  char orderId[50];
  double value;
  char paymentMethod[50]
} 
```

```haskell
data Order = Order 
  { orderId       :: String
  , value         :: Double 
  , paymentMethod :: String 
  }
```

```scala
case class Order(orderId: String, value: Double, paymentMethod: String) 
```

---

# Sum types

- Not every language has these, although they superficially look like enums 

```haskell
data ClothingSize = Small | Medium | Large | XLarge 
```

```scala
sealed trait ClothingSize 
case object Small extends ClothingSize
case object Medium extends ClothingSize 
case object Large extends ClothingSize
case object XLarge extends ClothingSize
```

---

# Algebraic Data Types 

- allows you to mix sum and product types 
- super powerful to narrowly define your data! 

An example from Elm on the front end for what an http response may have: 

```haskell
data OrderResponse 
  = PurchaseSuccessful { newOrder :: Order}
  | PaymentFailed      { paymentProvider:: ProviderId
                       , failureMessage :: String 
                       }
  | NetworkError       { statusCode :: Int
                       , message :: String 
                       } 
```

- this is a *rich* description of the DSL for what you get back when you place an order 
- conveys a ton of info in a small amount of space (same file!)

---

# ADT in scala 

- for completeness for slides 

```scala
sealed trait OrderResponse

case class PurchaseSuccessful(newOrder: Order) extends OrderResponse
case class PaymentFailed(paymentProvider: ProviderId, failureMessage: String)
  extends OrderResponse
case class NetworkError(statusCode: Int, message: String) extends OrderResponse
```

--- 

# ADTs 

- encode your assumptions
- talk in your DSL 
- cheaply
- thoroghly

---

# Relationships between stuff 

- these are functions (or methods in OOP) 

In a dynamic language, you have to keep track of the relationship in your head: 

```javascript
// what does this even return?
function withdraw (userId, amount) { ... } 
```

In a typed language: 

```java
public String withdraw(string userId, Int amount) {..}
```

---

# Relationships between stuff 

In a typed language: 

```java
public String withdraw(string userId, Int amount) {..}
```

The above is ok, but it's kind of reading the wrong way around 

```scala
def withdraw(string: userId, amount: Int): String 
```

---

# The relationship 

In ML-like languages it's even more obvious 

```haskell
withdraw :: String -> Int -> String 
--|           ^        ^       ^
--|           |        |       |
--| Needs ----.--------.       |
--|                            |
--| Produces ------------------.
```

--- 

# Records of Functions

- OOP and FP have plenty of similarities

```scala
trait UserRepository { 
  def addUser(user: User): Future[Unit]
  def deleteUser(user: User): Future[Unit]
  def getUser(account: Account): Future[User]
}
```

in FP all you have is data, but you also have first class functions

```haskell
data UserRepository = UserRepository 
  { addUser    :: User -> IO ()
  , deleteUser :: User -> IO ()
  , getUser    :: Account -> IO User
  }
```

They are the same thing. 

---

# Relationships between stuff 

Looking at the type signature helps:

- understand what's going on (weak documentation) 
- raise code smells 

the type `String -> Int -> String` should be setting off alarm bells 

- unfortunately, we love stringly typed models at Hootsuite `:(` 

---

# The problem with stringly typed models 

Worlds vaguest type signature: 

```scala
def foo(s: String): String {..}
```

What is foo?  

- toUpper, toLower, abbreviate? 
- a bunch of maths followed by .toString? 
- an assembly compiler that runs a program that outputs a string? 

---

# Type signature problems in production

```haskell
buildFacebookPageProfiles :: String -> F List SocialProfileInfo
buildLocationPageProfiles :: List SocialProfileInfo -> String -> F List SocialProfileInfo 
getInstagramBusinessSocialProfile :: SocialProfileInfo -> String -> F SocialProfileInfo 

// later 
splitSocialProfiles :: List SocialProfileInfo -> (List SocialProfileInfo, List SocialProfileInfo)
```

- we've been cleaning it up but it's been needless confusing

---

# Types 

1. types help us with compiler errors (passing the wrong type) 
2. types, in any language, help us constrain the world of possibilities 

Do more of 2. 

---

# Types and Cardinality 

- Types can be _too big_ 
    - e.g. using Int to hold Http status code 
    - we have `2^31 -1` valid values for http status code in the type system 
- Types are a _set_ and have  _cardinality_ 
- The _cardinality_ of a type should _fit the business requirement_

--- 

# Types and Cardinality

Remember we have first class functions, so 

```
A => B // a function from A to B 
```

is also a _type_ and you can think of constructors as functions, so `|A => B|` is how many implementations exist 

---

# Types and Cardinality

```scala
def getCurrency(country: String): Option[String] = ???

def getCurrency(country: Country): Currency = ??? 
```

It's intuitive to see which has a lower cardinality 

---

# Keeping your types small

Two techniques here when designing types:

- Expansion
- Restriction

Clear examples are present when handling inputs that don't map to the business domain:

- we can push the responsibility forward (expansion)
    - the caller of the code can provide whatever value they want
    - some condition might fail 
    - caller of the code needs to handle that failure later on 

- we can push the responsibility backwards (restriction) 
    - We restrict the range of inputs we will provide 
    - Instead of taking in Int, we take in only Natural numbers 
    - Instead of taking in List, we take in NonEmptyList
    - The caller is responsible for constructing the right type 
        - the downstream programs no longer need to worry about invalid inputs 

---

# Type Safety

*When we restrict what we can do, it's easier to understand what we can do* 

- pushing safety forard doesn't make things simpler downstream unfortunately
    - example taking our DTOs full of Options and using them deep in our business logic
    - we love doing this `:(` 

- pushing safety backwards _does_ make things simpler downstream 
    - like turning your DTO into an internal domain model 
    - by using types like NonEmptyList 
    - by forcing the caller to provide the right thing 

The more precisely our types describe our program, the fewer ways it will go wrong.

---

# Good types in scala 

We love strings at HS `:(` how much better would life be if we had more of the following: 

```scala
case class UserId(id: String Refined NonEmpty) 
type TwitterHandle = String Refined StartsWith["@"] :: MaxSize[16] :: MinxSize[2]
case class TwitterUser(userId: UserId, handle: TwitterHandle, permissions: NonEmptyList[Permissions])
```

We could do this today. 


--- 

# Generics 

- scala allows for generic parameters (and something called Higher-Kinded types which we will cover later) 
- The whole reason why you see simple variable names/types: `a, A, f, xs`
   - you don't know anything about them

This is no scarier than Java.  Some random generic code from guice: 

```java
public BindingBuilder<T> toProvider(Provider<? extends T> provider) {
    return toProvider((javax.inject.Provider<T>) provider);
  }
```

or c#: 

```csharp
class NodeItem<T> where T : System.IComparable<T>, new() { }
class SpecialNodeItem<T> : NodeItem<T> where T : System.IComparable<T>, new() { }
```

---

# A better view of Generics 

- the more kinds of things something can *potentially be*, the less we can *reason about what it actually is*
   - think about this for a moment 
   - it's the opposite of what you think happens when you go generic 
- we want to choose representations that are *sufficiently abstract* because *abstraction* weirdly buys you *precision*

---

# A better view of generics 

How many implementation of each tpye signature? 

```scala
def foo(a: Int): Int
def foo(s: String): String
def foo[A](a: A): A
```

- making something more abstract makes it more precise
- freedom at one level leads to a restriction at another 

---

# Relationships between stuff and Context of stuff 

### imagine for a minute we don't have classes and are not running on the JVM 

---

# Setup 

- We have Types [A, B, C, etc.]
- We have functions (f) that maps from A to B
  - it doesn't do anything else (doesn't update a counter, call a db, save a file, print to screen)
  - output is determined by the input 
  - evaluating an expression always results in the same answer
  - we can always *inline* a function, or *factor* one out 

---

# Functions as values 

- You can even turn higher order functions into data (look at something called *defunctionalization*) 

If you squint, you can sort of see it in Scala. 

```scala
val x: Int => String = i => i.toString 

// equivalent to 

val x1 = new Function1[Int, String] { def apply(i: Int): Int = i.toString } 
```

look at *Monix Task* it's all data structures, not method calls. 

---


# Setup and down the rabbit hole

- We have Types [A, B, C, etc.]
- We have functions (f) that maps from A to B
  - it doesn't do anything else (doesn't update a counter, call a db, save a file, print to screen)
  - output is determined by the input 
  - evaluating an expression always results in the same answer
  - we can always *inline* a function, or *factor* one out 

We call these *pure functions* and this leads to....

---

# REFERENTIAL TRANSPARENCY 

- we can substitute a variable for the expression it's bound to
- we can introduce a new variable to factor out common sub-expressions 
- for any expression, we can replace its value without changing the program's behavior 

```scala
val area = (radius: Int) => math.Pi * math.pow(radius, 2) 

val program = area(3) + area(4) 

val program1 = (math.Pi * math.pow(radius, 2)) + (math.Pi * math.pow(radius, 2))

// program1 is the same as program since it is referentially transparent 

// obviously not referentially transparent
var total = 0 
def addToToal(x: Int): Int = {
  total += x
  total 
}
```

---

# REFERENTIAL TRANSPARENCY

- Are these two programs the same?

```scala
// program 1 
val a = <expr>
(a,a)

// program2 
(<expr>, <expr>) 
```

- in OOP, who knows what is going on in there. 
- in FP, the answer is always yes`*`.
  - this is SUPER useful. We build on top of this everywhere. It allows us to _reason locally_ about what is going on. 

---

# REFERENTIAL TRANSPARENCY

- is this true? 

```scala
List(1,2,3,4).map(_ - 1).map(_ + 1) == List(1,2,3,4) 
```

---

# FP

- to the left of an equal sign is a name, to the right is the expression

```scala
val add_one = x => x + 1 // add_one is equal to the expression that appears on the right 
```

- functional programs are evaluation of _expressions_ 
    - NOT a sequence of statements which is what we are mostly used to
- running a program means we are evaluating an expression
- we build bigger programs out of composing smaller ones (function composition)
- we understand what going on by repeated use of substituting expressions 

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

- this is a good idea *in all languages* 
- side effecting functions (anything NOT a pure function) 
- talking to the outside world, updating DBs, etc.
- there are some tricks to wrap your head around the fact that you can still have referential transparency with side effecting functions 

---

# != Pure 

## Non-deterministic functions

remember types and functions are sets! 

```
input / domain              output / codomain 

--------------              ------------------
|   a1 -----------------------> b1 
|
|   a2 -----------------------> b2
|                 |
                  |___________> b3 

--------------              ------------------
```

```scala
import scala.util.Random
Random.nextInt(100) // 28
Random.NextInt(100) // 17
..
```

---

# != Pure

## Partial Function

```
input / domain              output / codomain 

--------------              ------------------
    a1 -----------------------> b1 

    a2 -----------------------> b2
   
    a3 

    a4 -----------------------> b4

--------------              ------------------

```

```scala
def head(xs: List[Int]): Int = xs match {
  case Nil => sys.error("empty list")
  case x :: _ => x 
}
```

---

# != Pure

## Null

```scala

def addOne(x: Int): Int = x + 1

addOne(null) / <---- booooom, scala.Matcherror: null
```

---

# != Pure
## Reflection 

```scala
def foo[A](a: A): A = a match {
  case x: Int    => (x + 1).asInstanceOf[A]
  case x: String => x.reverse.asInstanceOf[A]
  case _         => a
}

foo(5) // 6
foo("hello") // String = olleH
foo(true) // Boolean = true 
```

Wait this looks like it works

---

# != Pure
## Reflection

or not :( 

```scala
def foo[A](a: A): Int = a match {
  case _: List[Int]    => 0
  case _: List[String] => 1
  case _               => 2
}

foo(List(1,2,3,)) // Int = 0  YAY 
foo(List("abc"))  // Int = 0  BOO we expected 1, type erasure :( we match to the first part of the list 
```

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

# Contexts 

These are all a context: 

- Partiality? 
- Exceptions?
- Nondeterminism?
- Dependency injection?
- Logging? 
- Mutable state? 
- IO :: Side-effects (a program of pure functions does nothing, we need to talk to the outside world) 

They exist in every language 

*A language with an expressive type makes them explicit *

---

# Consequence of Referential Transparency

- Remember pure functions allow us to describe everything as _expressions_ 
    - not a _list of instructions_
- Running a functional program is evaluating an expression to a value 
- We build bigger programs out of smaller ones by _composing_ them
- If it's not _referential transparent_ then it's a _side-effect_
    - Can you do the substituition or not? 
- _effects_ and _side-effects_ are different

---

# Why do we care? 

- this let's us do bigger substituitions and find other properties that hold
  across generics we can rely on.  This will come up later when people talk
  about typeclass _laws_ (part 3)
---

# Effects - first tools in our toolbox 

- effect/context is a vague term
- we want to know what is in common about them

---

# Option

Gives us a way to represent we don't have an answer for you
intuition: exceptions, sort of

```scala
sealed trait Option[+A]
case object None extends Option[Nothing]
case class Some[A](a: A) extends Option[A]

// intuition: functions that may not yield an answer (partiality)

val f: A => Option[B]
val g: B => Option[C]

// but we can't compose them 
f andThen g // type error
```

If we had a way to smash them together in composition but we can't.

---

# Either

- intuition: functions may fail with a reason 
- intuition: this kind of gives us exceptions back (even moreso than *option*) 

```scala
sealed trait Either[+A, +B]
case class Left[+A, +B](a: A) extends Either[A, B]
case class Right[+A, +B](b: B) extends Either[A, B]

val f: A => Either[String, B]
val g: B => EIther[String, C]

// we can't compose them :( 
f andThen g // type error 
```

- if you could compose them and you got a C out you know they both worked
- but if something failed, then you'd get the first failure 
- but you can't really compose them like regular functions

---

# List 

- Yes! List is an effect. It's `listness` 
- Intuition: a kind of nondeterminism
- Intuition: define functions that might have many possible answers 

```scala
sealed trait List[+A]
case object Nil extends List[Nothing]
case class ::[+A](head: A, tail: List[A]): Extends List[A]

val f: A => List[B]
val g: B => List[C]

// we can't compose them
f andThen g // type error
```

- intuition: define functions that give any number of answers 
- intuition: composition would mean _every possible answer_ we could get
- this is a weird way to think of List 

---

# Future

- Intuition: concurrency, something happening somewhere else

```scala
def getStuff(a: User): Future[Permissions] = 
  for {

     response <- httpRequest(..)
     permissions <- parsePermissions(response) 
  } yield permissions
```

---

# So what do they have in common? 

- they all compute some sort of answer _with some extra stuff associated with them_ 
- the _extra stuff_ is what we call an _effect_

They all have the same Shape F[A]

```scala

type F[A] = Option[A]
type F[A] = Either[E, A] // for any fixed E 
type F[A] = List[A]
type F[A] = Reader[E, A] // for any type E
type F[A] = Writer[W, A] // for any type W 
type F[A] = State[S, A]  // for any type S 

// intuition: this extends to other "effects" 
type F[A] = Future[A] 
type F[A] = Task[A]
type F[A] = Validation[E, A] // for any type E 
```

- whatever distinguishes F[A] from A 
- F[A] is sometimes called a Context 
- there are many, many, many of these 
- they share many commonalities 
- sometimes F[A] is called "a program in F that computes a value of A" 
- sometimes F[A] is called a computation 

---

# Even more constraints 

- The problem with an effect like `Future` is that it's very vague
- It's the string of the effect world 
- what is `Future[Permission]` doing? 
  - hitting a DB (probably), 
  - hitting some kafka audit service for DB access? 
  - logging
  - hitting some microservice Mat wrote 6 months ago that no one really knows about 
  - mining bitcoin

- Much like Java, `Future[]` doesn't give us strong guarantees

---

# OAuth Example

Say we want to have some sort of Signed Request for seceurity purposes
```haskell
// java
// is this blocking? on a new thread? who knows, it could be deleting files!
signOauth :: Oauth -> Credential -> Request -> Request 

// not really any better in Scala 
signOauth :: Oauth -> Credential -> Requeste -> Future Request 
```

---

# OAuth Example - introduce a constraint at the type level 

- intuition restrict the `context` that is available 

```haskell
signOauth :: MonadOAuth m => OAuth -> Credential -> Request -> m Request 
```

```scala
trait SignedOAuth[F[_]] {
  def signRequest(oauth: OAuth, 
                   cred: Credeential,
                   req: Requset): F[Request]
}

def getSigned[F[_] : Monad : SignedOAuth]: F[Request] = 
  for { 
  signed <- signRequest(...)
  } yield signed 

// assuming an instance of MonadOAuth for Future, the above would work
```

This is called Final Tagless style 

---

# Constraints at a type level 

- unfortunatly, final tagless is just a social construct in scala
- the JVM allows too many escap hatches to get around it 
    - doesn't mean it's not a good idea though! 
- it can still be a good idea, some people do use it 
- alternatives are things like ZIO which have a bit stronger gaurantees 

- stronger type systems like haskell do this much better, and get `Effect Systems` 

---

# The FP value prop 

Using scala you get a language: 

- highly expressive domain modelling compared to go/java/c# 
    - first class functions 
    - concise generics 
    - support for ADTs and pattern matching
    - typeclasses over inheritance (talk 3)
        - also can do compile time function derivation 
- reasonably fast (JVM) 
- can be reasonably type safe
    - lots of compile side type magic in our libraries for: 
        - nice json reading/writing
        - refinement types 
        - type-safe database queries 
- not only model your types but model the context in which you are running 
    - Having a `List[Future[Option[Result]]` is a strength, not a messy type signature 

---

# When to use scala  

- if you are just doing simple IO/Crud:
    - the overhead of context tracking is probably not worth it (your app is mostly IO) 
    - you don't need an expressive domain modelling language 
    - use something else (honestly Java + Springboot will be hard to beat here) 

- if you have a good mixture of IO/interesting domain logic then scala really starts to shine 
- some excellent concurrency primatives in the FP world, if that is your problem space 

I'm really happy to be able to use Scala in my day job.

---

# Scala/FP Downsides

- very different mental model
- onboarding, onboarding, onboarding 
- mixed bag for learning resources
    - but it's getting better all the time 
- doesn't stop you from writing bad code 
- we have lots of bad scala laying around 
- mixed FP/OOP footguns
    - F-bounded polymorphism over typeclasses `:(` 
    - runtime DI doesn't jive with much of the compile time FP  

Scala's type system _affords you a rich language to express your DSL_ but if you don't use it, you don't get the benefit.

--- 

# Resources 

## General 

- These two talks by Kris Jenkins are mostly about elm but honestly are the best FP valueprop talks
    - [Communicating in types](https://www.youtube.com/watch?v=R2afqbzWDiU)
    - [Types as a design tool](https://www.youtube.com/watch?v=6mUAvd6i4OU)

---

## BOOKS
In this order: 

- [Essential Scala](https://books.underscore.io/essential-scala/essential-scala.html)
- the first 3-4 chapters of Functional And Reactive Domain Modeling 
- [Scala With Cats](https://underscore.io/books/scala-with-cats/)
- [Practical FP in Scala](https://leanpub.com/pfp-scala)

For something different, if you like the front end, check out elm 

- [elm in action](https://www.manning.com/books/elm-in-action)

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
    - practical haskell ... 
- fp for mere mortals
- the sky is limit...

---

# Leftover slides

---

# Back to Function Composition

```scala
def andThen[A,B,C](f: A => B, g: B => C): A => C = a => g(f(a))
```

For this _generic_ function we can start thinking about _laws_ (contracts!) that
hold true: 

Remember function coposition `(f . g)(x) = f(g(x))`

```scala
// right association
f andThen (g andThen h) =  
a => (g andThen h)(f(a))
a => (b => h(g(b)))(f(a))  // now apply f(a) for into the fn b => 
a => h(g(f(a)))

// left association
(f andThen g) andThen h = 
a => h (f andThen g)(a)
a => h (b => g(f(b)))(a) // now apply a into fn b => .. 
a => h (g(f(a))) 
```

We've proved this! This is a fact. A property. It's one more substituition we
can do in our program (even if this isn't the most interesting right now) 


