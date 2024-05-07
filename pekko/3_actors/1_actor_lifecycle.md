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