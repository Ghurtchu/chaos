  *Consistency*:
-----
distributed systems often replicate data across multiple nodes for fault tolerance and performance reasons. Maintaining consistency among these replicas is essential to ensure that all clients see a coherent view of the data. Different consistency models, such as strong consistency, eventual consistency, and causal consistency, provide varying guarantees about when and how updates are propagated across replicas to maintain consistency.

  *Fault tolerance*:
-----
a reliable distributed system should be designed to withstand failures at various levels, including hardware failures (e.g., disk crashes, server failures), software failures (e.g., bugs, crashes), and network failures (e.g., packet loss, latency spikes). Fault tolerance mechanisms such as redundancy, replication, and error detection and recovery techniques are typically employed to ensure that the system remains operational despite these failures.

  *Reliability*:
-----
an ability of the system to consistently deliver correct and consistent results, even in the presence of various types of failures, such as hardware failures, network outages, or software errors.
  
  *Scalable*:
-----
a scalable distributed system should be able to efficiently accommodate growth in terms of data volume, user requests, or any other workload metric, while maintaining acceptable levels of performance, reliability, and response time. This typically involves designing the system architecture in a way that allows for easy horizontal or vertical scaling, depending on the specific requirements and constraints of the system.

  *Latency*:
-----
refers to the time it takes for a message, request, or data packet to travel from its source to its destination. It is a measure of the delay experienced in communication between different components or nodes within the system. Latency can be influenced by various factors, including network congestion, distance between nodes, processing time at intermediate routers or servers, and the efficiency of the underlying communication protocols.  

  *Cluster Partition*:
-----
a cluster partition occurs when a distributed system splits into separate segments or partitions due to network failures, communication issues, or other factors. Essentially, it means that the nodes in the cluster are no longer able to communicate with each other effectively, leading to separate groups of nodes operating independently. This can result in isolated islands of nodes, where each island forms its own subset of the original cluster. Cluster partitions can pose challenges for data consistency and system availability in distributed systems.  