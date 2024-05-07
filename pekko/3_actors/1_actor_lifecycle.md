An actor is a stateful resource that has to be explicitly started and stopped.

It is important to note that actors do not stop automatically when no longer referenced, every Actor that is created must also explicitly be destroyed. 
The only simplification is that stopping a parent Actor will also recursively stop all the child Actors that this parent has created. 
All actors are also stopped automatically when the ActorSystem is shut down.

IMPORTANT NOTE: An ActorSystem is a heavyweight structure that will allocate threads, so create one per logical application. Typically one ActorSystem per JVM process.

# Creating Actors

An actor can create, or spawn, an arbitrary number of child actors, which in turn can spawn children of their own, thus forming an actor hierarchy.
ActorSystem hosts the hierarchy and there can be only one root actor, an actor at the top of the hierarchy of the ActorSystem.
The lifecycle of a child actor is tied to the parent – a child can stop itself or be stopped at any time but it can never outlive its parent.

# The ActorContext
The ActorContext can be accessed for many purposes such as:
- Spawning child actors and supervision
- Watching other actors to receive a Terminated(otherActor) event should the watched actor stop permanently
- Logging
- Creating message adapters
- Request-response interactions (ask) with another actor
- Access to the self ActorRef

If a behavior needs to use the ActorContext, for example to spawn child actors, or use context.self, it can be obtained by wrapping construction with Behaviors.setup:
```scala
object HelloWorldMain {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      val greeter = context.spawn(HelloWorld(), "greeter")

      Behaviors.receiveMessage { message =>
        val replyTo = context.spawn(HelloWorldBot(max = 3), message.name)
        greeter ! HelloWorld.Greet(message.name, replyTo)
        Behaviors.same
      }
    }
}
```

# ActorContext Thread Safety

Many of the methods in ActorContext are not thread-safe and:
- Must not be accessed by threads from scala.concurrent.Future callbacks
- Must not be shared between several actor instances
- Must only be used in the ordinary actor message processing thread

# The Guardian Actor

The top level actor, also called the user guardian actor, is created along with the ActorSystem. Messages sent to the actor system are directed to the root actor.
The root actor is defined by the behavior used to create the ActorSystem, named HelloWorldMain in the example below:

```scala
val system: ActorSystem[HelloWorldMain.SayHello] =
  ActorSystem(HelloWorldMain(), "hello")

system ! HelloWorldMain.SayHello("World")
system ! HelloWorldMain.SayHello("Pekko")
```

For very simple applications the guardian may contain the actual application logic and handle messages. As soon as the application handles more than one concern the guardian should instead just bootstrap the application, spawn the various subsystems as children and monitor their lifecycles.

When the guardian actor stops this will stop the ActorSystem.

When ActorSystem.terminate is invoked the Coordinated Shutdown process will stop actors and services in a specific order.

# Spawning Children
Child actors are created and started with ActorContext’s spawn. In the example below, when the root actor is started, it spawns a child actor described by the HelloWorld behavior. 
Additionally, when the root actor receives a SayHello message, it creates a child actor defined by the behavior HelloWorldBot:

```scala
object HelloWorldMain {

  final case class SayHello(name: String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup { context =>
      val greeter = context.spawn(HelloWorld(), "greeter")

      Behaviors.receiveMessage { message =>
        val replyTo = context.spawn(HelloWorldBot(max = 3), message.name)
        greeter ! HelloWorld.Greet(message.name, replyTo)
        Behaviors.same
      }
    }

}
```

To specify a dispatcher when spawning an actor use DispatcherSelector. If not specified, the actor will use the default dispatcher, see Default dispatcher for details.

```scala
def apply(): Behavior[SayHello] =
  Behaviors.setup { context =>
    val dispatcherPath = "pekko.actor.default-blocking-io-dispatcher"

    val props = DispatcherSelector.fromConfig(dispatcherPath)
    val greeter = context.spawn(HelloWorld(), "greeter", props)

    Behaviors.receiveMessage { message =>
      val replyTo = context.spawn(HelloWorldBot(max = 3), message.name)

      greeter ! HelloWorld.Greet(message.name, replyTo)
      Behaviors.same
    }
  }
```

# SpawnProtocol
The guardian actor should be responsible for initialization of tasks and create the initial actors of the application, but sometimes you might want to spawn new actors from the outside of the guardian actor. 
For example creating one actor per HTTP request.

That is not difficult to implement in your behavior, but since this is a common pattern there is a predefined message protocol and implementation of a behavior for this. 
It can be used as the guardian actor of the ActorSystem, possibly combined with Behaviors.setup to start some initial tasks or actors.
Child actors can then be started from the outside by telling or asking SpawnProtocol.Spawn to the actor reference of the system.
Using ask is similar to how ActorSystem.actorOf can be used in classic actors with the difference that a Future of the ActorRef is returned.

The guardian behavior can be defined as:

```scala
import org.apache.pekko
import pekko.actor.typed.Behavior
import pekko.actor.typed.SpawnProtocol
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.scaladsl.LoggerOps

object HelloWorldMain {
  def apply(): Behavior[SpawnProtocol.Command] =
    Behaviors.setup { context =>
      // Start initial tasks
      // context.spawn(...)

      SpawnProtocol()
    }
}
```

and the ActorSystem can be created with that main behavior and asked to spawn other actors:

```scala
import org.apache.pekko
import pekko.actor.typed.ActorRef
import pekko.actor.typed.ActorSystem
import pekko.actor.typed.Props
import pekko.util.Timeout


implicit val system: ActorSystem[SpawnProtocol.Command] =
  ActorSystem(HelloWorldMain(), "hello")

// needed in implicit scope for ask (?)
import pekko.actor.typed.scaladsl.AskPattern._
implicit val ec: ExecutionContext = system.executionContext
implicit val timeout: Timeout = Timeout(3.seconds)

val greeter: Future[ActorRef[HelloWorld.Greet]] =
  system.ask(SpawnProtocol.Spawn(behavior = HelloWorld(), name = "greeter", props = Props.empty, _))

val greetedBehavior = Behaviors.receive[HelloWorld.Greeted] { (context, message) =>
  context.log.info2("Greeting for {} from {}", message.whom, message.from)
  Behaviors.stopped
}

val greetedReplyTo: Future[ActorRef[HelloWorld.Greeted]] =
  system.ask(SpawnProtocol.Spawn(greetedBehavior, name = "", props = Props.empty, _))

for (greeterRef <- greeter; replyToRef <- greetedReplyTo) {
  greeterRef ! HelloWorld.Greet("Pekko", replyToRef)
}
```

# Stopping Actors
An actor can stop itself by returning Behaviors.stopped as the next behavior.

A child actor can be forced to stop after it finishes processing its current message by using the stop method of the ActorContext from the parent actor. Only child actors can be stopped in that way.

All child actors will be stopped when their parent is stopped.

When an actor is stopped, it receives the PostStop signal that can be used for cleaning up resources.

Here is an illustrating example:

```scala
import org.apache.pekko
import pekko.actor.typed.Behavior
import pekko.actor.typed.scaladsl.Behaviors
import pekko.actor.typed.{ ActorSystem, PostStop }


object MasterControlProgram {
  sealed trait Command
  final case class SpawnJob(name: String) extends Command
  case object GracefulShutdown extends Command

  def apply(): Behavior[Command] = {
    Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case SpawnJob(jobName) =>
            context.log.info("Spawning job {}!", jobName)
            context.spawn(Job(jobName), name = jobName)
            Behaviors.same
          case GracefulShutdown =>
            context.log.info("Initiating graceful shutdown...")
            // Here it can perform graceful stop (possibly asynchronous) and when completed
            // return `Behaviors.stopped` here or after receiving another message.
            Behaviors.stopped
        }
      }
      .receiveSignal {
        case (context, PostStop) =>
          context.log.info("Master Control Program stopped")
          Behaviors.same
      }
  }
}

object Job {
  sealed trait Command

  def apply(name: String): Behavior[Command] = {
    Behaviors.receiveSignal[Command] {
      case (context, PostStop) =>
        context.log.info("Worker {} stopped", name)
        Behaviors.same
    }
  }
}
```

# Watching Actors

In order to be notified when another actor terminates (i.e. stops permanently, not temporary failure and restart), an actor can watch another actor.
It will receive the Terminated signal upon termination (see Stopping Actors) of the watched actor.

```scala

object MasterControlProgram {
  sealed trait Command
  final case class SpawnJob(name: String) extends Command

  def apply(): Behavior[Command] = {
    Behaviors
      .receive[Command] { (context, message) =>
        message match {
          case SpawnJob(jobName) =>
            context.log.info("Spawning job {}!", jobName)
            val job = context.spawn(Job(jobName), name = jobName)
            context.watch(job)
            Behaviors.same
        }
      }
      .receiveSignal {
        case (context, Terminated(ref)) =>
          context.log.info("Job stopped: {}", ref.path.name)
          Behaviors.same
      }
  }
}
```
An alternative to watch is watchWith, which allows specifying a custom message instead of the Terminated. 
This is often preferred over using watch and the Terminated signal because additional information can be included in the message that can be used later when receiving it.

Similar example as above, but using watchWith and replies to the original requester when the job has finished.

```scala

object MasterControlProgram {
  sealed trait Command
  final case class SpawnJob(name: String, replyToWhenDone: ActorRef[JobDone]) extends Command
  final case class JobDone(name: String)
  private final case class JobTerminated(name: String, replyToWhenDone: ActorRef[JobDone]) extends Command

  def apply(): Behavior[Command] = {
    Behaviors.receive { (context, message) =>
      message match {
        case SpawnJob(jobName, replyToWhenDone) =>
          context.log.info("Spawning job {}!", jobName)
          val job = context.spawn(Job(jobName), name = jobName)
          context.watchWith(job, JobTerminated(jobName, replyToWhenDone))
          Behaviors.same
        case JobTerminated(jobName, replyToWhenDone) =>
          context.log.info("Job stopped: {}", jobName)
          replyToWhenDone ! JobDone(jobName)
          Behaviors.same
      }
    }
  }
}
```

It should be noted that the terminated message is generated independent of the order in which registration and termination occur.
In particular, the watching actor will receive a terminated message even if the watched actor has already been terminated at the time of registration.

Registering multiple times does not necessarily lead to multiple messages being generated, but there is no guarantee that only exactly one such message is received: if termination of the watched actor has generated and queued the message, and another registration is done before this message has been processed, then a second message will be queued, because registering for monitoring of an already terminated actor leads to the immediate generation of the terminated message.

It is also possible to deregister from watching another actor’s liveliness using context.unwatch(target). This works even if the terminated message has already been enqueued in the mailbox; after calling unwatch no terminated message for that actor will be processed anymore.

The terminated message is also sent when the watched actor is on a node that has been removed from the Cluster.