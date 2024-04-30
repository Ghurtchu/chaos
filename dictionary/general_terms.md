  *Consistency*
Distributed systems often replicate data across multiple nodes for fault tolerance and performance reasons. Maintaining consistency among these replicas is essential to ensure that all clients see a coherent view of the data. Different consistency models, such as strong consistency, eventual consistency, and causal consistency, provide varying guarantees about when and how updates are propagated across replicas to maintain consistency.

  *Fault tolerance*
A reliable distributed system should be designed to withstand failures at various levels, including hardware failures (e.g., disk crashes, server failures), software failures (e.g., bugs, crashes), and network failures (e.g., packet loss, latency spikes). Fault tolerance mechanisms such as redundancy, replication, and error detection and recovery techniques are typically employed to ensure that the system remains operational despite these failures.

  *Reliability*
an ability of the system to consistently deliver correct and consistent results, even in the presence of various types of failures, such as hardware failures, network outages, or software errors.

  *Scalable* 
a scalable distributed system should be able to efficiently accommodate growth in terms of data volume, user requests, or any other workload metric, while maintaining acceptable levels of performance, reliability, and response time. This typically involves designing the system architecture in a way that allows for easy horizontal or vertical scaling, depending on the specific requirements and constraints of the system.