package com.example;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.handler.annotation.SendTo;

@SpringBootApplication
@EnableBinding(Processor.class)
public class MetricsApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(MetricsApplication.class, args);
		context.getBean(RabbitTemplate.class).convertAndSend("metricsIn", "foo", "foo");
	}

	@StreamListener(Processor.INPUT)
	@SendTo(Processor.OUTPUT)
	public String process(String in) {
		System.out.println(in);
		return in.toUpperCase();
	}

	@StreamListener(Processor.OUTPUT)
	public void upper(String in) {
		System.out.println(in);
	}

}
