##spring-integration-vertx

###Intro

Demonstrates a bridge between a vert.x WebSockets server and a Spring Integration flow.

The bridge is made to "look" like a TcpConnectionFactory, anabling it to be hooked into existing inbound and outbound channel adapters.

The example serves up a simple web page that enables an

    <int:inbound-channel-adapter/>

to be started and stopped (by sending start and stop respectively).

When the adapter is started, it sends an incrementing integer once per second to the page over the web socket.

Multiple browser tabs/instances can be run concurrently and started/stopped independently.


A minor patch to vert.x removes a classpath check...

https://github.com/garyrussell/vert.x/commit/15ba56dfdc5eb50bd9958eb71307e5cdfbc18bd8

...so far this does not appear to have affected anything.

###Maven

The project uses maven for dependency resolution; vert.x is not mavenized so, after building vert.x with the above patch, you need to manually install 3 jars into your maven repo...

    mvn install:install-file -DgroupId=tim.fox -DartifactId=vertx -Dversion=1.0.0 -Dpackaging=jar -Dfile=/path/to/vert.x/target/dist-build/vert.x-1.0/lib/java/vert.x.jar

    mvn install:install-file -DgroupId=tim.fox -DartifactId=js -Dversion=1.0.0 -Dpackaging=jar -Dfile=/path/to/vert.x/target/dist-build/vert.x-1.0/lib/java/js.jar

    mvn install:install-file -DgroupId=tim.fox -DartifactId=netty -Dversion=1.0.0 -Dpackaging=jar -Dfile=/path/to/vert.x/target/dist-build/vert.x-1.0/lib/java/netty.jar


###Running the example

     mvn package -P WebSockets

(Or set up an STS launcher with the parameters shown in the pom &lt;profile/&gt;)

Open a web browser at

    http://localhost:8080/


Send 'start' and you'll see the counter incrementing; 'stop' stops the counter (restarting currently starts at 1 again).


