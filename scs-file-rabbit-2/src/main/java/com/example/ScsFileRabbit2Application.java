package com.example;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.listener.ListenerContainerIdleEvent;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;

@EnableBinding(Sink.class)
@SpringBootApplication
public class ScsFileRabbit2Application {

	public static void main(String[] args) {
		SpringApplication.run(ScsFileRabbit2Application.class, args);
	}

	@Bean
	public HandleRabbit fileToRabbit() {
		return new HandleRabbit();
	}

	public static class HandleRabbit {

		private static final Log logger = LogFactory.getLog(HandleRabbit.class);

		@Autowired
		private ApplicationContext context;

		@ServiceActivator(inputChannel = Processor.INPUT)
		public void handler(Map<String, String> input) {
			String queueName = input.get("queueName");
			if (queueName == null) {
				logger.error("No queue name");
				return;
			}
			logger.info("Processing: " + queueName);
			SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(
					this.context.getBean(CachingConnectionFactory.class));
			container.setQueueNames(queueName);
			container.setMessageListener(new MessageListenerAdapter(new Listener()));
			container.setIdleEventInterval(10000);
			final CountDownLatch latch = new CountDownLatch(1);
			container.setApplicationEventPublisher(new ApplicationEventPublisher() {

				@Override
				public void publishEvent(Object event) {
				}

				@Override
				public void publishEvent(ApplicationEvent event) {
					if (event instanceof ListenerContainerIdleEvent) {
						latch.countDown();
					}
				}

			});
			container.afterPropertiesSet();
			container.start();
			try {
				latch.await(); // need a get out of jail card here
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			logger.info("Container idle - stopping");
			container.stop();
			RabbitAdmin admin = this.context.getBean(RabbitAdmin.class); // need to figure out why @Autowired doesn't work
			admin.deleteQueue(queueName);
		}

	}

	public static class Listener {

		public void handleMessage(String line) {
			System.out.println(line);
		}

	}

}
