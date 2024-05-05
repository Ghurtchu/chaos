Where are actors located and how they are distributed / identified within Pekko cluster?

We may have a Cluster of 3 nodes - (3 actor systems, basically 3 JVM-s) and their hosts may look like:
- `pekko://sys@host_a:2552`
- `pekko://sys@host_b:2552`
- `pekko://sys@host_c:2552`

Where `pekko` is a protocol name, `sys@host_*` is a host name and `2552` is port.

We know that during `ActorSystem` startup pekko creates `system` and `user` guardians.

app devs create actor hierarchy under `user` guardian and it may look like:
- `pekko://sys@host_a:2552/user` - `user` guardian
- `pekko://sys@host_a:2552/user/a` - child actor of `user`
- `pekko://sys@host_a:2552/user/a/b` - child actor of `a`

What is an `ActorRef[_]`?

`ActorRef`'s purpose is to have the ability to send messages to other actors. Actors have reference to self through `ActorContext.self`.

- local actor refs are intended to sending messages within the same JVM or ActorSystem
- local actor refs, if remoting is enabled can send messages to different JVM-s or ActorSystems / nodes as long as serialization protocol and remote actor address is configured
- remote actor refs are reachable suing remote communication using serialization

What is an `Actor Path`?

It's an unique identifier of an actor which looks like a file system. It starts with anchor and concatenated actor names (see above).

examples:
- `"pekko://my-sys/user/service-a/worker1"`               // purely local
- `"pekko://my-sys@host.example.com:5678/user/service-b"` // remote

The interpretation of the host and port part (i.e. host.example.com:5678 in the example) depends on the transport mechanism used, but it must abide by the URI structural rules.

The unique path obtained by following the parental supervision links towards the root guardian is called the logical actor path. This path matches exactly the creation ancestry of an actor, so it is completely deterministic as soon as the actor system’s remoting configuration (and with it the address component of the path) is set.

When an actor is terminated, its reference will point to the dead letter mailbox, DeathWatch will publish its final transition and in general it is not expected to come back to life again (since the actor life cycle does not allow this).

What is address part used for? Simple - for sending messages to remote actors, the path is used to find out the location of the actor and to which actor system / JVM it belongs to so that msg is forwarded there.

Top level scopes for actor paths:
- `/`: root guardian, parent of all
- `/user`: all actors created using `ActorSystem.actorOf` are children of `user`
- `/system`: is the guardian actor for all system-created top-level actors, e.g. logging listeners or actors automatically deployed by configuration at the start of the actor system.
- `/deadLetters`: is the dead letter actor, which is where all messages sent to stopped or non-existing actors are re-routed (on a best-effort basis: messages may be lost even within the local JVM).
- `/temp`: is the guardian for all short-lived system-created actors, e.g. those which are used in the implementation of ActorRef.ask.
- `/remote`: is an artificial path below which all actors reside whose supervisors are remote actor references

