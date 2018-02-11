# What is this?

A demonstration of implementing "Commands", a mechanism to "guarantee", within limit ;-)data consistency across
multiple microservices. See blog article http://blog.maxant.co.uk/pebble/TODO.

More information at [../src](CommandService)

Based on Swarm application with:

- JAX-RS
- CDI
- EJB
- JPA
- JTA
- Project Stages (Configuration)
- Flyway
- Tests using H2, Prod with Mysql

Build and run:

    mvn clean install && java -jar target/demo-swarm.jar

Run tests using Mysql, rather than in-memory H2:

   mvn test -Dtest.use.mysql

#Useful Links

- https://wildfly-swarm.gitbooks.io/wildfly-swarm-users-guide/content/
- [System Properties](https://wildfly-swarm.gitbooks.io/wildfly-swarm-users-guide/content/configuration_properties.html)

# Issues

- none

# TODO

- how come after unit tests with Mysql, there are still entries in the DB?
-- which test is it?
- cors: https://github.com/wildfly-swarm/wildfly-swarm-examples/blob/master/jaxrs/health/src/main/java/org/wildfly/swarm/examples/jaxrs/health/CORSFilter.java
- modify case should create a task in ARO which fires an event. modification should also fire an event.
- move common stuff to parent
- add a process manager monkey which kills instances randomly and triple check we never lose data!
