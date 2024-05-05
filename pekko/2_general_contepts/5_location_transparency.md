Pekko is distributed by default, so the transparent remoting is enabling the location transparency by distributing the sending of messages 
to potentially remote actors which may live on the different node (different JVM / actor system which are managed within Pekko cluster).

The most obvious restriction is that all messages sent over the wire must be serializable and are slower than local sends.

Another consequence is that everything needs to be aware of all interactions being fully asynchronous, which in a computer network might mean that it may take several minutes for a message to reach its recipient (depending on configuration). It also means that the probability for a message to be lost is much higher than within one JVM, where it is close to zero (still: no hard guarantee!).

Pekko Remoting connects different actor systems in peer-to-peer fashion.

Important: Using setups involving Network Address Translation, Load Balancers or Docker containers violates assumption 1, unless additional steps are taken in the network configuration to allow symmetric communication between involved systems. In such situations Pekko can be configured to bind to a different network address than the one used for establishing connections between Pekko nodes.

In addition to being able to run different parts of an actor system on different nodes of a cluster, it is also possible to scale up onto more cores by multiplying actor sub-trees which support parallelization (think for example a search engine processing different queries in parallel). The clones can then be routed to in different fashions, e.g. round-robin. See Routing for more details.