class MyActor(context: ActorContext[MyActor.Command]) extends AbstractBehavior[MyActor.Command](context) {
  import MyActor._

  var state = ""
  val mySet = mutable.Set[String]()

  def onMessage(cmd: MyActor.Command) = cmd match {
    case Message(text, otherActor) =>
      // Very bad: shared mutable object allows
      // the other actor to mutate your own state,
      // or worse, you might get weird race conditions
      otherActor ! mySet

      implicit val ec = context.executionContext

      // Example of incorrect approach
      // Very bad: shared mutable state will cause your
      // application to break in weird ways
      Future { state = "This will race" }

      // Example of incorrect approach
      // Very bad: shared mutable state will cause your
      // application to break in weird ways
      expensiveCalculation().foreach { result =>
        state = s"new state: $result"
      }

      // Example of correct approach
      // Turn the future result into a message that is sent to
      // self when future completes
      val futureResult = expensiveCalculation()
      context.pipeToSelf(futureResult) {
        case Success(result) => UpdateState(result)
        case Failure(ex)     => throw ex
      }

      // Another example of incorrect approach
      // mutating actor state from ask future callback
      import org.apache.pekko.actor.typed.scaladsl.AskPattern._
      implicit val timeout: Timeout = 5.seconds // needed for `ask` below
      implicit val scheduler = context.system.scheduler
      val future: Future[String] = otherActor.ask(Query(_))
      future.foreach { result =>
        state = result
      }

      // use context.ask instead, turns the completion
      // into a message sent to self
      context.ask[Query, String](otherActor, Query(_)) {
        case Success(result) => UpdateState(result)
        case Failure(ex)     => throw ex
      }
      this

    case UpdateState(newState) =>
      // safe as long as `newState` is immutable, if it is mutable we'd need to
      // make a defensive copy
      state = newState
      this
  }
}