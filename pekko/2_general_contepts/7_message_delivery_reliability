Pekko builds distributed apps which can scale up and out.
Scale up - using multiple processor cores for doing work
Scale out - making it distributed on the network (clustering, sharding and other good stuff)

key thing is message passing among actors, that's why we need to discuss message passing semantics

if actors are on different nodes (clustering) then the message will have more latency than if actors were within the same actor system or same jvm

remote message send sets the limit on the message size and more things can go wrong there.

Pekko has at-most-once delivery, i.e. no guaranteed delivery



