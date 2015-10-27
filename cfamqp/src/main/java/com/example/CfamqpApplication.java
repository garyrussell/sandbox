package com.example;

import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StopWatch;

@SpringBootApplication
public class CfamqpApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(CfamqpApplication.class)
			.run(args);
		RabbitTemplate template = context.getBean(RabbitTemplate.class);
		String exchange = context.getBean(DirectExchange.class).getName();
		BlockingQueue<?> q = context.getBean(BlockingQueue.class);
		int count = 4000;
		int rate = 100;
		int actualRate = 0;
		int out = 0;
		StopWatch watch = new StopWatch();
		System.out.println("Sending " + count + " at " + rate + "/s (notional)");
		try {
			watch.start("sends");
			for (int i = 0; i < count; i++) {
				template.convertAndSend(exchange, "foo", "foo" + i);
				Thread.sleep(1000 / rate);
			}
			watch.stop();
			actualRate = (int) (((double) count) / watch.getLastTaskTimeMillis() * 1000);
			watch.start("receives");
			while (out++ < count) {
				if (q.poll(10, TimeUnit.SECONDS) == null) {
					throw new RuntimeException("Insufficient results");
				}
			}
			watch.stop();
		}
		finally {
//			context.close();
			System.out.println(watch.prettyPrint());
			System.out.println("Actual send rate " + actualRate + "/s");
		}

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
	public RabbitAdmin admin(ConnectionFactory connectionFactory) {
		return new RabbitAdmin(connectionFactory);
	}

	@Bean
	public LinkedBlockingQueue<String> results() {
		return new LinkedBlockingQueue<String>();
	}

	@Bean
	public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, LinkedBlockingQueue<String> results) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		container.setQueues(queue());
		container.setMessageListener(new MessageListenerAdapter(new Object() {
			@SuppressWarnings("unused")
			public void handleMessage(String message) {
				results.add(message);
			}
		}));
		return container;
	}

}
