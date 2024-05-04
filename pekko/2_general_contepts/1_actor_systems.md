Actors encapsulate state and behaviour. They send messages to each other. Actors have mailboxes and are the stringent form of OOP. 

Actors can form an organizational and hierarchical structure for solving problems.

Actors create child actors for making tasks more manageable and delegate.

Actors are defined in terms of what messages they process, how they react and what happens when they fail.

If an actor carries very important data, it's good idea to create children actors to delegate further tasks and handle their failures, if any. Maybe it's a good idea to have a new child per request - "Error kernel pattern".

If one actor depends on another actor for carrying out its duty, it should watch that other actor’s liveness and act upon receiving a termination notice.

If one actor has multiple responsibilities each responsibility can often be pushed into a separate child to make the logic and state more simple, but be careful not to create too many actors, try choosing an appropriate granularity based on intuition and domain.

The actor system as a collaborating ensemble of actors is the natural unit for managing shared facilities like scheduling services, configuration, logging, etc. Several actor systems with different configurations may co-exist within the same JVM without problems, there is no global shared state within Pekko itself, however the most common scenario will only involve a single actor system per JVM. Couple this with the transparent communication between actor systems — within one node or across a network connection — and actor systems are a perfect fit to form a distributed application.

Best practices:
- process commands and generate responses
- actors should not block
- do not pass mutable objects between actors
- prefer immutable messages
- avoid sending behaviour within messages
- prefer hierarchical actors for benefiting from fault handling



When you know everything is done for your application, you can have the user guardian actor stop, or call the terminate method of ActorSystem. That will run CoordinatedShutdown stopping all running actors.

If you want to execute some operations while terminating ActorSystem, look at CoordinatedShutdown.