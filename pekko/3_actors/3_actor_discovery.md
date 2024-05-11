# Obtaining Actor references

There are two general ways to obtain Actor references: by creating actors and by discovery using the Receptionist.

You can pass actor references between actors as constructor parameters or part of messages.

Sometimes, you need something to bootstrap the interaction, for example when actors are running on different nodes in the Cluster or when “dependency injection” with constructor parameters is not applicable.

# Receptionist

