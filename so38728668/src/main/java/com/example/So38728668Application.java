package com.example;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.Header;

import com.rabbitmq.client.Channel;

@SpringBootApplication
@EnableRabbit
public class So38728668Application {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(So38728668Application.class, args);
		context.getBean(RabbitTemplate.class).convertAndSend("", "so38728668", "foo");
		context.getBean(Listener.class).latch.await(60, TimeUnit.SECONDS);
		context.close();
	}

	@Bean
	public Queue so38728668() {
		return new Queue("so38728668");
	}

	@Bean
	public Listener listener() {
		return new Listener();
	}

	public static class Listener {

		private final CountDownLatch latch = new CountDownLatch(1);

		@RabbitListener(queues = "so38728668")
		public void receive(String payload, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag)
				throws IOException {
			System.out.println(payload);
			channel.basicAck(tag, false);
			latch.countDown();
		}

	}

}
