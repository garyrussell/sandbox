package com.example;

import java.util.Scanner;
import java.util.UUID;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RabbitrpcApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(RabbitrpcApplication.class, args);
		final Scanner scanner = new Scanner(System.in);
		String exchangeName = context.getBean(DirectExchange.class).getName();
		RabbitTemplate template = context.getBean(RabbitTemplate.class);
		System.out.println("Type a message and hit return");
		while (scanner.hasNext()) {
			String in = scanner.nextLine();
			System.out.println(template.convertSendAndReceive(exchangeName, "foo", in));
			System.out.println("Type a message and hit return");
		}
		scanner.close();
		context.close();
	}

	@Bean
	public Queue queue() {
		return new AnonymousQueue();
	}

	@Bean
	public DirectExchange exchange() {
		return new DirectExchange(UUID.randomUUID().toString(), false, true);
	}

	@Bean
	public Binding binding() {
		return BindingBuilder.bind(queue()).to(exchange()).with("foo");
	}

	@Bean
	public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		container.setQueues(queue());
		container.setPrefetchCount(1);
		container.setMessageListener(new MessageListenerAdapter(new Object() {
			@SuppressWarnings("unused")
			public String handleMessage(String message) {
				return message.toUpperCase();
			}
		}));
		return container;
	}

}
