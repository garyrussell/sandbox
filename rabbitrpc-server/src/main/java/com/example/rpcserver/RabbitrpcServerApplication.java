package com.example.rpcserver;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RabbitrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(RabbitrpcServerApplication.class, args);
	}

	@Bean
	public Queue queue() {
		return new Queue("rpc.requests");
	}

	@Bean
	public DirectExchange exchange() {
		return new DirectExchange("rpc.exchange");
	}

	@Bean
	public Binding binding() {
		return BindingBuilder.bind(queue()).to(exchange()).with("rpc");
	}

	// Classic Listener Container
//	@Bean
//	public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory) {
//		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
//		container.setQueues(queue());
//		container.setPrefetchCount(10);
//		container.setMessageListener(new MessageListenerAdapter(new Object() {
//			@SuppressWarnings("unused")
//			public String handleMessage(String message) {
//				return message.toUpperCase();
//			}
//		}));
//		return container;
//	}

	// POJO Listener
	@Bean
	public Listener listener() {
		return new Listener();
	}

	public static class Listener {

		@RabbitListener(queues="rpc.requests")
		public String upCase(String in) {
			return in.toUpperCase();
		}

 	}

}
