List of most common Pekko libraries and their goals:
- `Actor library`: 
- `Remoting`
- `Cluster`
- `Cluster Sharding`
- `Cluster Singleton`
- `Persistence`
- `Projections`
- `Distributed Data`
- `Streams`
- `Pekko Connectors`
- `HTTP`
- `gRPC`
- `other Pekko modules`


  *`Actor library`*

The core Pekko library, `pekko-actor-typed`, introduces actors as a fundamental programming paradigm for building high-performance, concurrent, and distributed systems. Unlike traditional OOP, where encapsulation is limited to state, actors encapsulate both state and execution. Communication with actors occurs through message passing rather than method calls, enabling efficient concurrency and remote communication. Actors address challenges such as building high-performance applications, handling errors in multi-threaded environments, and mitigating concurrency pitfalls. This approach offers a consistent, integrated model across Pekko libraries, simplifying concurrent and distributed system design.

  *`Remoting`*

Remoting in Pekko facilitates seamless message exchange between actors residing on different computers. It operates as a module rather than a traditional library, primarily configured rather than interacted with through APIs. Leveraging the actor model, it ensures uniformity between local and remote message sends. Remoting addresses challenges such as addressing remote actor systems and individual actors, serialization of messages, managing network connections, detecting failures, and multiplexing communications transparently. While direct usage of Remoting is infrequent, it serves as the foundation for the Cluster subsystem within Pekko.

  *`Cluster`*

The Cluster module in Pekko facilitates the organized management of actor systems collaborating to solve business problems. Unlike Remoting, which addresses remote communication, Clustering orchestrates these systems into a cohesive "meta-system" governed by a membership protocol. It offers additional services crucial for real-world applications. The Cluster module tackles challenges such as maintaining communication among clustered systems, safely introducing new members, detecting unreachable systems, handling system failures, distributing computations among members, and designating specific roles to cluster members. Overall, it ensures disciplined management and effective coordination within a cluster of actor systems.

  *`Cluster Sharding`*

Sharding in Pekko addresses the distribution of actors across a cluster, particularly in conjunction with Persistence for managing large sets of persistent entities. It tackles challenges related to scaling out stateful entities, balancing load across machines, migrating entities from failed systems without data loss, and maintaining data consistency by ensuring that entities exist on only one system at a time. Sharding optimizes resource utilization and ensures fault tolerance in distributed systems.

  *`Cluster Singleton`*

The Singleton module in distributed systems addresses the need for a single entity responsible for a task shared among cluster members, ensuring fault tolerance and accessibility. Despite potentially creating a bottleneck, this pattern is sometimes necessary. The Singleton module resolves challenges such as guaranteeing only one instance of a service across the cluster, ensuring service availability during system failures or scaling down, and enabling access to the service from any cluster member, even as it migrates across systems. It optimizes resource utilization and ensures consistent service availability in distributed environments.

  *`Persistence`*

Persistence in the context of actors provides mechanisms for storing events that represent changes in state, enabling actors to recover their state upon system restarts or crashes. Similar to the principles of Command Query Responsibility Segregation (CQRS), this approach allows for querying event streams and feeding them into additional processing pipelines or alternate views. Persistence addresses challenges such as restoring actor state after system failures, implementing CQRS systems, ensuring reliable message delivery despite network errors or crashes, introspecting domain events, and leveraging Event Sourcing for supporting long-running processes while maintaining project evolution. It enhances fault tolerance, scalability, and data integrity in distributed systems.

  *`Projections`*

Projections in Pekko offer a streamlined API for consuming event streams and projecting them into various downstream options. While the core dependency provides only the API, additional provider dependencies are needed for different source and sink implementations. Projections address challenges such as constructing alternate or aggregate views over an event stream, propagating an event stream onto another downstream medium like a Kafka topic, and providing a straightforward method for building read-side projections within the context of Event Sourcing and CQRS systems. Overall, Projections simplify the process of handling event streams and enable flexible downstream processing options, enhancing the scalability and versatility of distributed systems.

  *`Distributed Data`*

The Distributed Data module in Pekko facilitates data sharing between nodes in a cluster, even in the presence of cluster partitions, by leveraging Conflict Free Replicated Data Types (CRDTs). CRDTs allow concurrent writes on different nodes, which are then merged predictably afterward. The module provides infrastructure and various data types for efficient data sharing. It addresses challenges such as accepting writes during cluster partitions and ensuring low-latency local read and write access while sharing data. Overall, Distributed Data enables eventual consistency and resilient data sharing in distributed systems.

  *`Streams`*

Streams in Pekko provide a higher-level abstraction on top of actors, simplifying the creation of processing networks for sequential event streams or large datasets. They handle concurrency, resource usage, and coordination between stages in the network, making it easier to write efficient and scalable processing pipelines. Streams also implement the Reactive Streams standard, enabling seamless integration with third-party implementations. Key challenges addressed by Streams include managing streams of events or datasets with high performance, assembling reusable processing components into flexible pipelines, connecting asynchronous services efficiently, and providing or consuming Reactive Streams compliant interfaces for interoperability with other libraries. Overall, Streams offer a safe, typed, and composable programming model for building reactive and high-performance systems.
  
  *`Pekko Connectors`*

Pekko Connectors, distinct from the main Pekko module, offers a collection of modules built on the Streams API. These provide Reactive Stream connector implementations for various common technologies in the cloud and infrastructure realm. Pekko Connectors address challenges such as connecting infrastructure or persistence components to Stream-based flows and interfacing with legacy systems while adhering to the Reactive Streams API. For more detailed information, refer to the Pekko Connectors overview page.

  *`HTTP`*

Pekko HTTP, a distinct module from Pekko, offers tools for constructing or consuming HTTP services, which are vital for providing APIs remotely. It provides capabilities to create and serve HTTP services, as well as a client for consuming other services. Particularly well-suited for streaming large datasets or real-time events, Pekko HTTP leverages the underlying model of Pekko Streams. This module addresses challenges such as exposing services to the external world via an HTTP API in a performant manner and streaming data and live events in and out of a system using HTTP.

  *`gRPC`*

Pekko gRPC, a standalone module, offers an implementation of gRPC that seamlessly integrates with the HTTP and Streams modules. It facilitates the generation of client and server-side artifacts from protobuf service definitions, enabling the exposure and handling of services through Pekko HTTP and Streams. Pekko gRPC addresses challenges such as providing services with the benefits of gRPC and protobuf, including schema-first contract, schema evolution support, efficient binary protocol, first-class streaming support, wide interoperability, and the use of HTTP/2 connection multiplexing.