Two categories of exceptions in actor:
- input validation failure, expected exceptions
- unexpected failures, I/O or disk writing failure, an app bug

Use supervision only for the second case stuff. Supervision doesn't care about business logic.

Supervision can do 3 things:
- resume the actor, keeping the state
- restart the actor, clear the state
- stop the actor permanently

Each ActorSystem starts at least three top level actors:
- `/`: root guardian, parent of `user` and `system` actors
- `/user`: child of root and parent of all the actors you create 
- `/system`: child of root and parent of all the actors for pekko internals

Actors can monitor other actors for such failures. 
Not only parent actors can watch children. 
One actor may react to another actor's termination.

Lifecycle monitoring is implemented by `Terminated` message which is received by monitoring actor.

How to subscribe / unsubscribe for this message
- `ActorContext.watch(actorRef)`
- `ActorContext.unwatch(actorRef)`

What happens during exceptions?

During processing the message let's say DB operation may fail

If this exception occurs during msg processing then the msg is lost. 

It's not put back into mailbox, if we wanna retry this operation due to failure we gotta use try/catch or other programming constructs and re-run the flow but make sure you put the upper bound to the retries to avoid livelock (consuming lots of cpu cycles without progressing).

during exception mailbox is safe.

during exception actor is suspended and supervision is started, so:
- it may continue with the last state
- wipe out state and restart
- terminate fully
