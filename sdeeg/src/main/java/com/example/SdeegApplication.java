package com.example;

import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SdeegApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = SpringApplication.run(SdeegApplication.class, args);
		RabbitTemplate template = ctx.getBean(RabbitTemplate.class);
		for (int i = 0; i < 20; i++) {
			template.convertAndSend("sdeeg", "foo" + i);
		}
	}

	@Bean
	public SimpleMessageListenerContainer container(ConnectionFactory cf) {
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(cf);
		container.setQueueNames("sdeeg");
		container.setMessageListener(
				(MessageListener) m -> {
					try {
						System.out.println(m);
						Thread.sleep(1000);
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
		container.setPrefetchCount(10);
		container.setTxSize(10);
		return container;
	}

	@Bean
	public Queue sdeeg() {
		return new Queue("sdeeg");
	}

}
