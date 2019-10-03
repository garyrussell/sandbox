package com.example.demo;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.AbstractRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class So58087354Application {

	public static void main(String[] args) {
		SpringApplication.run(So58087354Application.class, args);
	}

	@RabbitListener(queues = "foo")
	public void listen(String in) {
		System.out.println("here");
		throw new NullPointerException("Test");
	}

}

@Component
class ContainerRetryConfigurer {

	ContainerRetryConfigurer(AbstractRabbitListenerContainerFactory<?> factory) {
		factory.setAdviceChain(RetryInterceptorBuilder.stateless()
				.maxAttempts(2)
				.backOffOptions(1000, 1.0, 1000)
				.build());
	}

}
