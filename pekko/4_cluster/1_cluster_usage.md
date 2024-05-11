# Cluster Usage

You have to enable serialization to send messages between ActorSystems (nodes) in the Cluster. Serialization with Jackson is a good choice in many cases, and our recommendation if you don’t have other preferences or constraints.

# Cluster API Extension

The Cluster extension gives you access to management tasks such as `Joining`, `Leaving` and `Downing` and subscription of cluster membership events such as `MemberUp`, `MemberRemoved` and `UnreachableMember`, which are exposed as event APIs.

It does this through these references on the Cluster extension:
- manager: An `ActorRef[cluster.typed.ClusterCommand]` where a `ClusterCommand` is a command such as: `Join`, `Leave` and `Down`
- subscriptions: An `ActorRef[cluster.typed.ClusterStateSubscription]` where a `ClusterStateSubscription` is one of `GetCurrentState` or `Subscribe` and `Unsubscribe` to cluster events like `MemberRemoved`
- state: The current `CurrentClusterState`

All of the examples below assume the following imports:

```scala
import org.apache.pekko
import pekko.actor.typed._
import pekko.actor.typed.scaladsl._
import pekko.cluster.ClusterEvent._
import pekko.cluster.MemberStatus
import pekko.cluster.typed._
```

The minimum configuration required is to set a host/port for remoting and the 
`pekko.actor.provider = "cluster".`

```sbt
pekko {
  actor {
    provider = "cluster"
  }
  remote.artery {
    canonical {
      hostname = "127.0.0.1"
      port = 7354
    }
  }

  cluster {
    seed-nodes = [
    "pekko://ClusterSystem@127.0.0.1:7354",
    "pekko://ClusterSystem@127.0.0.1:7355"]

    downing-provider-class = "org.apache.pekko.cluster.sbr.SplitBrainResolverProvider"
  }
}
```

Accessing the `Cluster` extension on each node:

```scala
val cluster = Cluster(system)
```

**NOTE**: The name of the cluster’s `ActorSystem` must be the same for all members, which is passed in when you start the ActorSystem.

# Joining and Leaving a Cluster

If not using configuration to specify seed nodes to join, joining the cluster can be done programmatically via the manager.

```scala
cluster.manager ! Join(cluster.selfMember.address)
```

`Leaving` the cluster and downing a node are similar:

```scala
cluster2.manager ! Leave(cluster2.selfMember.address)
```

# Cluster Subscriptions

`Cluster subscriptions` can be used to receive messages when cluster state changes. For example, registering for all `MemberEvent`’s,
then using the manager to have a node leave the cluster will result in events for the node going through the `Membership` Lifecycle.

This example subscribes to a subscriber: `ActorRef[MemberEvent]`:
```scala
cluster.subscriptions ! Subscribe(subscriber, classOf[MemberEvent])
```

Then asking a node to leave:
```scala
cluster.manager ! Leave(anotherMemberAddress)
// subscriber will receive events MemberLeft, MemberExited and MemberRemoved
```

# Cluster Membership API

# Joining

The seed nodes are initial contact points for joining a cluster, which can be done in different ways:

- automatically with `Cluster Bootstrap`
- with configuration of `seed-nodes`
- programatically

After the joining process the seed nodes are not special and they participate in the cluster in exactly the same way as other nodes.

# Joining automatically to seed nodes with Cluster Bootstrap
Automatic discovery of nodes for the joining process is available using the open source Pekko Management project’s module, `Cluster Bootstrap`. Please refer to its documentation for more details.

# Joining configured seed nodes
When a new node is started it sends a message to all seed nodes and then sends a join command to the one that answers first. If none of the seed nodes replies (might not be started yet) it retries this procedure until success or shutdown.

You can define the seed nodes in the configuration file (`application.conf`):
```sbt
pekko.cluster.seed-nodes = [
  "pekko://ClusterSystem@host1:7355",
  "pekko://ClusterSystem@host2:7355"
]
```

The seed nodes can be started in any order. It is not necessary to have all seed nodes running, but the node configured as the first element in the seed-nodes list must be started when initially starting a cluster. If it is not, the other seed-nodes will not become initialized, and no other node can join the cluster. The reason for the special first seed node is to avoid forming separated islands when starting from an empty cluster. It is quickest to start all configured seed nodes at the same time (order doesn’t matter), otherwise it can take up to the configured seed-node-timeout until the nodes can join.

As soon as more than two seed nodes have been started, it is no problem to shut down the first seed node. If the first seed node is restarted, it will first try to join the other seed nodes in the existing cluster. Note that if you stop all seed nodes at the same time and restart them with the same seed-nodes configuration they will join themselves and form a new cluster, instead of joining remaining nodes of the existing cluster. That is likely not desired and can be avoided by listing several nodes as seed nodes for redundancy, and don’t stop all of them at the same time.

If you are going to start the nodes on different machines you need to specify the ip-addresses or host names of the machines in application.conf instead of 127.0.0.1

# Joining programmatically to seed nodes

Joining programmatically is useful when dynamically discovering other nodes at startup through an external tool or API.

The seed node address list has the same semantics as the configured seed-nodes, and the underlying implementation of the process is the same, see Joining configured seed nodes.

When joining to seed nodes you should not include the node itself, except for the node that is supposed to be the first seed node bootstrapping the cluster. The desired initial seed node address should be placed first in the parameter to the programmatic join.

# Tuning joins
Unsuccessful attempts to contact seed nodes are automatically retried after the time period defined in configuration property seed-node-timeout. Unsuccessful attempts to join a specific seed node are automatically retried after the configured retry-unsuccessful-join-after. Retrying means that it tries to contact all seed nodes, then joins the node that answers first. The first node in the list of seed nodes will join itself if it cannot contact any of the other seed nodes within the configured seed-node-timeout.

The joining of given seed nodes will, by default, be retried indefinitely until a successful join. That process can be aborted if unsuccessful by configuring a timeout. When aborted it will run Coordinated Shutdown, which will terminate the ActorSystem by default. CoordinatedShutdown can also be configured to exit the JVM. If the seed-nodes are assembled dynamically, it is useful to define this timeout, and a restart with new seed-nodes should be tried after unsuccessful attempts.

```sbt
pekko.cluster.shutdown-after-unsuccessful-join-seed-nodes = 20s
pekko.coordinated-shutdown.exit-jvm = on
```

An actor system can only join a cluster once, additional attempts will be ignored. Once an actor system has successfully joined a cluster, it would have to be restarted to join the same cluster again. It can use the same host name and port after the restart. When it come up as a new incarnation of an existing member in the cluster and attempts to join, the existing member will be removed and its new incarnation allowed to join.

# Leaving
There are a few ways to remove a member from the cluster.

- The recommended way to leave a cluster is a graceful exit, informing the cluster that a node shall leave. This is performed by Coordinated Shutdown when the ActorSystem is terminated and also when a SIGTERM is sent from the environment to stop the JVM process.
- Graceful exit can also be performed using HTTP or JMX.
- When a graceful exit is not possible, for example in case of abrupt termination of the JVM process, the node will be detected as unreachable by other nodes and removed after Downing.

Graceful leaving offers faster hand off to peer nodes during node shutdown than abrupt termination and downing.

The `Coordinated` Shutdown will also run when the cluster node sees itself as Exiting, i.e. leaving from another node will trigger the shutdown process on the leaving node. Tasks for graceful leaving of cluster, including graceful shutdown of Cluster Singletons and Cluster Sharding, are added automatically when Pekko Cluster is used. For example, running the shutdown process will also trigger the graceful leaving if not already in progress.

Normally this is handled automatically, but in case of network failures during this process it may still be necessary to set the node’s status to Down in order to complete the removal, see Downing.

# Downing
In many cases a member can gracefully exit from the cluster, as described in Leaving, but there are scenarios when an explicit downing decision is needed before it can be removed. For example in case of abrupt termination of the JVM process, system overload that doesn’t recover, or network partitions that don’t heal. In such cases, the node(s) will be detected as unreachable by other nodes, but they must also be marked as Down before they are removed.

When a member is considered by the failure detector to be unreachable the leader is not allowed to perform its duties, such as changing status of new joining members to ‘Up’. The node must first become reachable again, or the status of the unreachable member must be changed to Down. Changing status to Down can be performed automatically or manually.

We recommend that you enable the Split Brain Resolver that is part of the Pekko Cluster module. You enable it with configuration:

```sbt
pekko.cluster.downing-provider-class = "org.apache.pekko.cluster.sbr.SplitBrainResolverProvider"
```

You should also consider the different available downing strategies.

If a downing provider is not configured downing must be performed manually using HTTP or JMX.

Note that `Cluster Singleton` or `Cluster Sharding` entities that are running on a crashed (unreachable) node will not be started on another node until the previous node has been removed from the Cluster. Removal of crashed (unreachable) nodes is performed after a downing decision.

Downing can also be performed programmatically with Cluster(system).manager ! Down(address), but that is mostly useful from tests and when implementing a DowningProvider.

If a crashed node is restarted and joining the cluster again with the same hostname and port, the previous incarnation of that member will first be downed and removed. The new join attempt with same hostname and port is used as evidence that the previous is no longer alive.

If a node is still running and sees its self as Down it will shutdown. Coordinated Shutdown will automatically run if run-coordinated-shutdown-when-down is set to on (the default) however the node will not try and leave the cluster gracefully.

# Node Roles

Not all nodes of a cluster need to perform the same function. For example, there might be one sub-set which runs the web front-end, one which runs the data access layer and one for the number-crunching. Choosing which actors to start on each node, for example cluster-aware routers, can take node roles into account to achieve this distribution of responsibilities.

The node roles are defined in the configuration property named `pekko.cluster.roles` and typically defined in the start script as a system property or environment variable.

The roles are part of the membership information in `MemberEvent` that you can subscribe to. The roles of the own node are available from the `selfMember` and that can be used for conditionally starting certain actors:
```scala
val selfMember = Cluster(context.system).selfMember
if (selfMember.hasRole("backend")) {
  context.spawn(Backend(), "back")
} else if (selfMember.hasRole("frontend")) {
  context.spawn(Frontend(), "front")
}
```

# Failure Detector Strategies

# Using the Failure Detector

Cluster uses the `remote.PhiAccrualFailureDetector` failure detector by default, or you can provide your by implementing the `remote.FailureDetector` and configuring it:

```html
pekko.cluster.implementation-class = "com.example.CustomFailureDetector"
```
In the Cluster Configuration you may want to adjust these depending on you environment:
- When a phi value is considered to be a failure pekko.cluster.failure-detector.threshold
- Margin of error for sudden abnormalities pekko.cluster.failure-detector.acceptable-heartbeat-pause

# How To Startup when a Cluster size is reached
A common use case is to start actors after the cluster has been initialized, members have joined, and the cluster has reached a certain size.

With a configuration option you can define the required number of members before the leader changes member status of ‘Joining’ members to ‘Up’.:
```html
pekko.cluster.min-nr-of-members = 3
```
In a similar way you can define the required number of members of a certain role before the leader changes member status of ‘Joining’ members to ‘Up’.:
```html
pekko.cluster.role {
frontend.min-nr-of-members = 1
backend.min-nr-of-members = 2
}
```


