##spring-integration-vertx

###Intro

Demonstrates the configuration of the vert.x library as a Spring Integration server TcpConnectionFactory, allowing bidirectional communication between an S.I. app and a browser WebSocket.

The example serves up a simple web page that enables an


    <int:inbound-channel-adapter/>

to be started and stopped (by sending start and stop respectively).

When the adapter is started, it sends an incrementing integer once per second to the page over the web socket.

Multiple browser tabs/instances can be run concurrently and started/stopped independently.


###Running the example

Run the Main program as a java application.


Open a web browser at

    http://localhost:8080/


Send 'start' and you'll see the counter incrementing; 'stop' stops the counter (restarting will resume from where we left off).


