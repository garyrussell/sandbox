package org.springframework.amqp.consumer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@SpringBootApplication
@EnableConfigurationProperties({ CloudInstanceProperties.class, ConsumerProperties.class })
public class RepartitionConsumerApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(RepartitionConsumerApplication.class, args);
		Thread.sleep(1000);
		context.getBean(Configurer.class).sendStarted();
	}

	@Configuration
	public static class Configurer implements EnvironmentAware, SmartLifecycle {

		private Environment environment;

		@Autowired
		private ConnectionFactory connectionFactory;

		@Autowired
		private AmqpTemplate template;

		@Autowired
		private CloudInstanceProperties instanceProperties;

		@Autowired
		private ConsumerProperties consumerProperties;

		private boolean running;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@Bean
		public TopicExchange exchange() {
			return new TopicExchange(consumerProperties.getDestination());
		}

		@Bean
		public Queue instanceQueue() {
			return new Queue(this.consumerProperties.getDestination() + ".instance."
					+ this.instanceProperties.getInstanceIndex(), false, false, true); // auto-delete
		}

		@Bean
		public Binding fooBinding() {
			return BindingBuilder.bind(instanceQueue()).to(exchange()).with("control.*");
		}

		@Bean
		public DirectMessageListenerContainer controlListener() {
			DirectMessageListenerContainer container = new DirectMessageListenerContainer(this.connectionFactory);
			container.setQueues(instanceQueue());
			container.setMessageListener(m -> {
				System.out.println("Received new control message " + m);
				if (isRunning()) {
					rebalance(m);
				}
			});
			return container;
		}

		private void rebalance(Message m) {
			System.out.println(
					new Date() + " Instance: " + this.instanceProperties.getInstanceIndex() + " Rebalancing...");
			List<String> queues = queues();
			int partitions = queues.size();
			System.out.println("partitions found: " + partitions);
			int instances = instanceMax().get() + 1;
			System.out.println(">>>>>>>>>>> instances found: " + instances);
			mainListener().removeQueueNames(mainListener().getQueueNames());
			int queuesPerInstance = partitions / instances;
			int myFirst = this.instanceProperties.getInstanceIndex() * queuesPerInstance;
			int myLast = Math.min((this.instanceProperties.getInstanceIndex() + 1) * queuesPerInstance - 1,
					partitions);
			if (partitions - myLast < queuesPerInstance) {
				myLast = partitions - 1;
			}
			String destinationPrefix = this.consumerProperties.getDestination() + "-";
			System.out.println("my first: " + destinationPrefix +  myFirst
					+ ", my last: " + destinationPrefix + myLast);
			mainListener().addQueueNames(Arrays.copyOfRange(queues.toArray(), myFirst, myLast + 1, String[].class));
			System.out.println("Rebalanced");
		}

		private List<String> queues() {
			return managementTemplate().getBindingsForExchange(virtualHost(), exchange().getName())
				.stream()
				.filter(b -> b.isDestinationQueue()
						&& b.getDestination().startsWith(this.consumerProperties.getDestination() + "-"))
				.map(b -> b.getDestination())
				.collect(Collectors.toList());
		}

		private Optional<Integer> instanceMax() {
			return managementTemplate().getBindingsForExchange(virtualHost(), exchange().getName())
				.stream()
				.filter(b -> b.isDestinationQueue()
						&& b.getDestination().startsWith(this.consumerProperties.getDestination() + ".instance."))
				.map(b -> Integer.parseInt(b.getDestination().substring(b.getDestination().lastIndexOf('.') + 1)))
				.max(Integer::compare);
		}

		@Bean
		public DirectMessageListenerContainer mainListener() {
			DirectMessageListenerContainer container = new DirectMessageListenerContainer(this.connectionFactory);
			container.setMessageListener(m -> {
				System.out.println("Received new message " + new String(m.getBody()));
			});
			container.setConsumerTagStrategy(q -> this.environment.getProperty("spring.application.name") + "."
					+ q + "." + this.instanceProperties.getInstanceIndex());
			return container;
		}

		public void sendStarted() {
			this.template.convertAndSend(exchange().getName(), "control." + this.instanceProperties.getInstanceIndex(),
					"started", m -> {
						m.getMessageProperties().getHeaders().put("instance",
								this.instanceProperties.getInstanceIndex());
						return m;
					});
		}

		public void sendStopped() {
			this.template.convertAndSend(exchange().getName(), "control." + this.instanceProperties.getInstanceIndex(),
					"stopped", m -> {
						m.getMessageProperties().getHeaders().put("instance",
								this.instanceProperties.getInstanceIndex());
						return m;
					});
		}

		@Bean
		public String virtualHost() {
			String virtualHost = environment.getProperty("cloud.services.rabbitmq.connection.virtualhost");
			if (virtualHost == null) {
				virtualHost = "/";
			}
			return virtualHost;
		}

		@Bean
		public RabbitManagementTemplate managementTemplate() {
			String uri = environment.getProperty("cloud.services.rabbitmq.connection.managementuri");
			String username = "guest";
			String password = "guest";
			if (uri == null) {
				uri = "http://guest:guest@localhost:15672/api/";
			}
			try {
				URI theUri = new URI(uri);
				String userInfo = theUri.getUserInfo();
				if (userInfo != null) {
					String[] userParts = userInfo.split(":");
					if (userParts.length > 0) {
						username = userParts[0];
					}
					if (userParts.length > 1) {
						password = userParts[1];
					}
				}
			}
			catch (URISyntaxException e) {
			}
			return new RabbitManagementTemplate(uri, username, password);
		}

		@Override
		public void start() {
			this.running = true;
		}

		@Override
		public void stop() {
			if (this.isRunning()) {
				this.running = false;
				sendStopped();
			}
		}

		@Override
		public boolean isRunning() {
			return this.running;
		}

		@Override
		public int getPhase() {
			return Integer.MAX_VALUE;
		}

		@Override
		public boolean isAutoStartup() {
			return true;
		}

		@Override
		public void stop(Runnable callback) {
			stop();
			callback.run();
		}

	}

}
