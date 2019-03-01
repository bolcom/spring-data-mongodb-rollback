# spring-data-mongodb-rollback
A distributed, optimistic, cross-microservice, generic transaction engine.

# Goal
In a microservices landscape, you often bump into the issue of one microservice having to reserve resources in other microservices, during which an error can occur. In these cases, it is expected that you release the resources already claimed in a reliable manner.
The goal of this library is to offer a featherweight solution to mark claimed resources across the landscape and be able to commit or roll back reliably, no matter what happens.
