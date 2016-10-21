package org.springframework.amqp.protonj;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class RepartitionApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(RepartitionApplication.class, args);
		TopicExchange exchange = context.getBean("foo", TopicExchange.class);
		RabbitAdmin admin = context.getBean(RabbitAdmin.class);
		admin.getQueueProperties("foo-0"); // kick start - creates a connection
		BindingsController controller = context.getBean(BindingsController.class);
		RabbitManagementTemplate rmt = controller.createTemplate();
		String virtualHost = controller.getVirtualHost();
		Optional<Integer> max = maxConsumerPartition(rmt, virtualHost);
		while (!max.isPresent()) {
			Thread.sleep(1000);
			max = maxConsumerPartition(rmt, virtualHost);
		}
		int currentMax = max.get();
		boolean stop = false;
		while (!stop) {
			Thread.sleep(5000);
			int newMax = maxConsumerPartition(rmt, virtualHost).get();
			System.out.println("New high partition: " + newMax);
			if (newMax > currentMax) {
				System.out.println("Partitions increased to " + newMax + ", rebalancing...");
			}
			else if (newMax < currentMax) {
				System.out.println("Partitions decreased to " + newMax + ", rebalancing...");
//				stop = true;
			}
			if (newMax == 1) {
				// simulate the highest instance going away
				admin.deleteQueue("foo-1");
			}
			else {
				// simulate a new instance starting up
				Queue foo1 = new Queue("foo-1");
				admin.declareQueue(foo1);
				admin.declareBinding(BindingBuilder.bind(foo1).to(exchange).with("foo-1"));
			}
			currentMax = newMax;
		}
		admin.deleteQueue("foo-0");
		admin.deleteExchange("foo");
		context.close();
	}

	private static Optional<Integer> maxConsumerPartition(RabbitManagementTemplate rmt, String virtualHost) {
		return rmt.getBindingsForExchange(virtualHost, "foo")
			.stream()
			.filter(b -> b.isDestinationQueue() && b.getDestination().startsWith("foo-"))
			.map(b -> Integer.parseInt(b.getDestination().substring(4)))
			.max(Integer::compare);
	}

	@Bean
	public TopicExchange foo() {
		return new TopicExchange("foo");
	}

	@Bean
	public Queue fooQueue() {
		return new Queue("foo-0");
	}

	@Bean
	public Binding fooBinding() {
		return BindingBuilder.bind(fooQueue()).to(foo()).with("foo-0");
	}

	@RestController
	@RequestMapping("/foo")
	public static class BindingsController implements EnvironmentAware {

		private Environment environment;

		@Override
		public void setEnvironment(Environment environment) {
			this.environment = environment;
		}

		@GetMapping(name = "foo")
		public String foo() throws Exception {
			StringBuilder builder = new StringBuilder();
			String virtualHost = getVirtualHost();
			RabbitManagementTemplate template = createTemplate();
			builder.append("<br/>" + template.getBindings(virtualHost));
			return builder.toString();
		}

		private String getVirtualHost() {
			String virtualHost = environment.getProperty("cloud.services.rabbitmq.connection.virtualhost");
			if (virtualHost == null) {
				virtualHost = "/";
			}
			return virtualHost;
		}

		public RabbitManagementTemplate createTemplate() {
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
			RabbitManagementTemplate template = new RabbitManagementTemplate(uri, username, password);
			return template;
		}

	}

}
