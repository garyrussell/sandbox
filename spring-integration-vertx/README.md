##spring-integration-vertx

###Intro

Demonstrates the configuration of the vert.x library as a Spring Integration server TcpConnectionFactory, allowing bidirectional communication between an S.I. app and a browser WebSocket.

The example serves up a simple web page that enables an


    <int:inbound-channel-adapter/>

to be started and stopped (by sending start and stop respectively).

When the adapter is started, it sends an incrementing integer once per second to the page over the web socket.

Multiple browser tabs/instances can be run concurrently and started/stopped independently.


###Maven

The project uses maven for (most of its) dependency resolution; vert.x is not currently mavenized so its libraries are added to the classpath manually - for eclipse/STS users, add a classpath variable VERTX_LIB pointing to the build directory - in my case:

    VERTX_LIB=/home/.../vert.x/target/dist-build/vert.x-1.0.beta1/lib/jars


###Running the example

Run the Main program as a java application. If you are not using STS, you will need the maven dependencies as well as the 3 jars from VERTX_LIB as can be seen in the eclipse .classpath file.


Open a web browser at

    http://localhost:8080/


Send 'start' and you'll see the counter incrementing; 'stop' stops the counter (restarting will resume from where we left off).


