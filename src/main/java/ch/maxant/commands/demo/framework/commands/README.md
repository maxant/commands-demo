# What is this package for?

We want to send data from this microservice to another microservice, via HTTP REST, in a robust and eventually consistent manner.
We have chosen to do this asynchronously, and we want to save the fact that we need to call another service as a command, in the same transaction as we save our business data, in order to guarantee atomicity (i.e. that we end up with either a) our data and the command both being saved, or b) neither being saved).

Saving such commands is a common problem, and by no means trivial.

Things to consider:

- If multiple instances of the microservice are running, how do we ensure that a command is only executed by one instance?
- What happens if execution fails?
- What happens if execution is partially successful, e.g. we make a call to a non-transactional resource like another microservice, but cannot commit that fact because of technical problems (e.g. this instance of this microservice dies; network failure to the database; business data constraint problem; etc.)

To solve these problems, we need a resource-locking mechanism to ensure only one instance will attempt to execute a command at a time and we need a retry mechanism for cases where an execution fails and we need a timeout mechansim for when resources are not released.

This package contains the necessary framework code in order to handle these issues.

Basically we use a "select for update" to reserve a set of commands for the currently running instance.
That update is committed in order to release the lock in the DB.
The reserved commands are then executed asynchronously by the container.
Any which are successful remove the command from the database.
Any which fail, increase the retry count.
After 5 retries, the framework gives up.
Currently there are no admin tools in this package for dealing with such cases but a) a log entry is created which informs of the problem and b) one could easily create an alert based on a simple SQL select which searches for rows which are reserved (i.e. contain a non-null value in the locked column) and have a retry count of at least 5.
Any commands which are reserved for more than 30 seconds are released so that any instance can re-attempt to execute the command.

Note: Resources called by the commands must support idempotency because we do not guarantee "exactly once delivery", rather "at least once delivery".

## Alternatives

### Central JMS Cluster

We could just use a remote JMS server and commit to it using an XA transaction.
This should work in practise and be quite reliable.
The chances of network failure between closely located microservice instances, a database cluster and message server cluster are small, but possible.
Attention needs to be paid to how the Java EE server manages the XA transaction as typically it writes a transaction log locally. If the instance dies / is removed and that involves the inability to access this transaction log during startup and recovery, then messages can be lost.
More likely when e.g. a Swarm service runs in a non-persistent Docker container, or where you use auto-scaling in Kubernetes.

### Local JMS Queue + Consumer

Similar to the above solution, but removes the problem of network connection failures between the Java EE server and the JMS server, because the JMS server runs inside the Java EE server.

The issue that can arise here, especially in a containerised solution, is that not only is the XA transaction log written locally, but so is the queue data.
The solution isn't really cloud enabled, because typically instances should be stateless (i.e. not write local files) and should be designed to be replaced simply.
If data consistency relies on local files not being lost between a failure and the following recovery, then it isn't compatible with a cloud based solution.
Also not compatible with Kubernetes auto-scaling.

### Nothing

We could just call the other microservice via HTTP. What happens if that is successul, but this microservice is unable to commit its data to the database?
We can end up with inconsistencies at a global level.
That is exactly the problem we are attempting to solve using the classes in this package.