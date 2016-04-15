package com.example;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BootMissingQueueApplication {

	public static void main(String[] args) {
		SpringApplication.run(BootMissingQueueApplication.class, args);
	}

	@Bean
	public SimpleMessageListenerContainer container(ConnectionFactory cf) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);
		container.setQueueNames("missing");
		container.setMessageListener(new MessageListenerAdapter(new Object() {

			@SuppressWarnings("unused")
			public void handle(Object in) {

			}

		}));
		return container;
	}

}
