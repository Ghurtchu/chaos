When an actor throws an unexpected exception, a failure, while processing a message or during initialization, the actor will by default be stopped.

NOTE: An important difference between Typed actors and Classic actors is that by default: the former are stopped if an exception is thrown and no supervision strategy is defined while in Classic they are restarted.

Note that there is an important distinction between failures and validation errors:

A **validation error** means that the data of a command sent to an actor is not valid, this should rather be modelled as a part of the actor protocol than make the actor throw exceptions.

A **failure** is instead something unexpected or outside the control of the actor itself, for example a database connection that broke. Opposite to validation errors, it is seldom useful to model failures as part of the protocol as a sending actor can very seldomly do anything useful about it.

For failures it is useful to apply the “let it crash” philosophy: instead of mixing fine grained recovery and correction of internal state that may have become partially invalid because of the failure with the business logic we move that responsibility somewhere else. For many cases the resolution can then be to “crash” the actor, and start a new one, with a fresh state that we know is valid.

Examples:

This example restarts the actor when it fails with an IllegalStateException:

```scala
Behaviors.supervise(behavior).onFailure[IllegalStateException](SupervisorStrategy.restart)
```

Or to resume, ignore the failure and process the next message, instead:
```scala
Behaviors.supervise(behavior).onFailure[IllegalStateException](SupervisorStrategy.resume)
```

More complicated restart strategies can be used e.g. to restart no more than 10 times in a 10 second period:
```scala
Behaviors
  .supervise(behavior)
  .onFailure[IllegalStateException](
    SupervisorStrategy.restart.withLimit(maxNrOfRetries = 10, withinTimeRange = 10.seconds))
```

To handle different exceptions with different strategies calls to supervise can be nested:
```scala
Behaviors
  .supervise(Behaviors.supervise(behavior).onFailure[IllegalStateException](SupervisorStrategy.restart))
  .onFailure[IllegalArgumentException](SupervisorStrategy.stop)
```

For a full list of strategies see the public methods on SupervisorStrategy.

NOTE: When the behavior is restarted the original Behavior that was given to Behaviors.supervise is re-installed, which means that if it contains mutable state it must be a factory via Behaviors.setup. When using the object-oriented style with a class extending AbstractBehavior it’s always recommended to create it via Behaviors.setup as described in Behavior factory method. For the function style there is typically no need for the factory if the state is captured in immutable parameters.

```scala
object Counter {
  sealed trait Command
  case class Increment(nr: Int) extends Command
  case class GetCount(replyTo: ActorRef[Int]) extends Command

  def apply(): Behavior[Command] =
    Behaviors.supervise(counter(1)).onFailure(SupervisorStrategy.restart)

  private def counter(count: Int): Behavior[Command] =
    Behaviors.receiveMessage[Command] {
      case Increment(nr: Int) =>
        counter(count + nr)
      case GetCount(replyTo) =>
        replyTo ! count
        Behaviors.same
    }
}
```

When doing this supervision only needs to be added to the top level:

```scala
def apply(): Behavior[Command] =
  Behaviors.supervise(counter(1)).onFailure(SupervisorStrategy.restart)
```

Each returned behavior will be re-wrapped automatically with the supervisor.

# Child actors are stopped when parent is restarting

Child actors are often started in a setup block that is run again when the parent actor is restarted. The child actors are stopped to avoid resource leaks of creating new child actors each time the parent is restarted.

```scala
def child(size: Long): Behavior[String] =
  Behaviors.receiveMessage(msg => child(size + msg.length))

def parent: Behavior[String] = {
  Behaviors
    .supervise[String] {
      Behaviors.setup { ctx =>
        val child1 = ctx.spawn(child(0), "child1")
        val child2 = ctx.spawn(child(0), "child2")

        Behaviors.receiveMessage[String] { msg =>
          // message handling that might throw an exception
          val parts = msg.split(" ")
          child1 ! parts(0)
          child2 ! parts(1)
          Behaviors.same
        }
      }
    }
    .onFailure(SupervisorStrategy.restart)
}
```

It is possible to override this so that child actors are not influenced when the parent actor is restarted. The restarted parent instance will then have the same children as before the failure.

If child actors are created from setup like in the previous example and they should remain intact (not stopped) when parent is restarted, the supervisee should be placed inside the setup and using SupervisorStrategy.restart.withStopChildren(false) like this:

```scala
def parent2: Behavior[String] = {
  Behaviors.setup { ctx =>
    val child1 = ctx.spawn(child(0), "child1")
    val child2 = ctx.spawn(child(0), "child2")

    // supervision strategy inside the setup to not recreate children on restart
    Behaviors
      .supervise {
        Behaviors.receiveMessage[String] { msg =>
          // message handling that might throw an exception
          val parts = msg.split(" ")
          child1 ! parts(0)
          child2 ! parts(1)
          Behaviors.same
        }
      }
      .onFailure(SupervisorStrategy.restart.withStopChildren(false))
  }
}
```

That means that the setup block will only be run when the parent actor is first started, and not when it is restarted.

# The PreRestart signal

Before a supervised actor is restarted it is sent the PreRestart signal giving it a chance to clean up resources it has created, much like the PostStop signal when the actor stops. The returned behavior from the PreRestart signal is ignored.

```scala
def withPreRestart: Behavior[String] = {
  Behaviors
    .supervise[String] {
      Behaviors.setup { ctx =>
        val resource = claimResource()

        Behaviors
          .receiveMessage[String] { msg =>
            // message handling that might throw an exception

            val parts = msg.split(" ")
            resource.process(parts)
            Behaviors.same
          }
          .receiveSignal {
            case (_, signal) if signal == PreRestart || signal == PostStop =>
              resource.close()
              Behaviors.same
          }
      }
    }
    .onFailure[Exception](SupervisorStrategy.restart)
}
```

Note that PostStop is not emitted for a restart, so typically you need to handle both PreRestart and PostStop to cleanup resources.

# Bubble failures up through the hierarchy

In some scenarios it may be useful to push the decision about what to do on a failure upwards in the Actor hierarchy and let the parent actor handle what should happen on failures (in classic Pekko Actors this is how it works by default).

For a parent to be notified when a child is terminated it has to watch the child. If the child was stopped because of a failure the ChildFailed signal will be received which will contain the cause. ChildFailed extends Terminated so if your use case does not need to distinguish between stopping and failing you can handle both cases with the Terminated signal.

If the parent in turn does not handle the Terminated message it will itself fail with an DeathPactException.

This means that a hierarchy of actors can have a child failure bubble up making each actor on the way stop but informing the top-most parent that there was a failure and how to deal with it, however, the original exception that caused the failure will only be available to the immediate parent out of the box (this is most often a good thing, not leaking implementation details).

There might be cases when you want the original exception to bubble up the hierarchy, this can be done by handling the Terminated signal, and rethrowing the exception in each actor.

