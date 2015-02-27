RabbitMQ Driver Using Spring AMQP `RabbitTemplate` or the native java `Channel`.

    $ mvn package
	
	$ java -jar target/rabbitdriver-0.0.1-SNAPSHOT.jar --count=500000 --queue=q1 --instances=2 --useTemplate=false
	
(command line args all required)

    count - number of messages per instance
	queue - queue prefix - will be appended with -n (where n is the instance)
	instances - how many instances - currently up to 50 supported
	useTemplate - whether to use the RabbitTemplate or not

