The Actor Model as defined by Hewitt, Bishop and Steiger in 1973 is a computational model that expresses exactly what it means for computation to be distributed. The processing units—Actors—can only communicate by exchanging messages and upon reception of a message an Actor can do the following three fundamental actions:

- send a finite number of messages to Actors it knows
- create a finite number of new Actors
- designate the behavior to be applied to the next message

An actor is a container for State, Behavior, a Mailbox, Child Actors and a Supervisor Strategy. All of this is encapsulated behind an Actor Reference. One noteworthy aspect is that actors have an explicit lifecycle, they are not automatically destroyed when no longer referenced; after having created one, it is your responsibility to make sure that it will eventually be terminated as well—which also gives you control over how resources are released When an Actor Terminates.

Actor Reference is an outer object which makes it easier and safer to share actor refs around.

`State`: Actors have state which makes them unique. 

The implementation detail of actors scheduling on threads are not important for users.

Because the internal state is vital to an actor’s operations, having inconsistent state is fatal. Thus, when the actor fails and is restarted by its supervisor, the state will be created from scratch, like upon first creating the actor. This is to enable the ability of self-healing of the system.

Optionally, an actor’s state can be automatically recovered to the state before a restart by persisting received messages and replaying them after restart (aka Event Sourcing).

`Behaviour`: a function which performs some actions based on point in time (State) and message type. Messages can contain actor references which makes actors communicate with each other easily.

`Mailbox`: each actor has exactly one mailbox to which all senders enqueue messages. Enqueuing happens in the time-order of send operations, which means that messages sent from different actors may not have a defined order at runtime due to the apparent randomness of distributing actors across threads. Sending multiple messages to the same target from the same actor, on the other hand, will enqueue them in the same order.

There are different mailbox implementations to choose from, the default being a FIFO: the order of the messages processed by the actor matches the order in which they were enqueued. This is usually a good default, but applications may need to prioritize some messages over others. In this case, a priority mailbox will enqueue not always at the end but at a position as given by the message priority, which might even be at the front. While using such a queue, the order of messages processed will naturally be defined by the queue’s algorithm and in general not be FIFO.

Actor can create children and supervise them. The list of children is within the actor's context. 

The final piece of an actor is its strategy for handling unexpected exceptions - failures. Fault handling is then done transparently by Pekko, applying one of the strategies described in Fault Tolerance for each failure.

Once an actor terminates, i.e. fails in a way which is not handled by a restart, stops itself or is stopped by its supervisor, it will free up its resources, draining all remaining messages from its mailbox into the system’s “dead letter mailbox” which will forward them to the EventStream as DeadLetters. The mailbox is then replaced within the actor reference with a system mailbox, redirecting all new messages to the EventStream as DeadLetters. This is done on a best effort basis, though, so do not rely on it in order to construct “guaranteed delivery”.
