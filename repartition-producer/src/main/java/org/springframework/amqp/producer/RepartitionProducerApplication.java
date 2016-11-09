package org.springframework.amqp.producer;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitManagementTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@EnableConfigurationProperties(ProducerProperties.class)
public class RepartitionProducerApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(RepartitionProducerApplication.class, args);
		TopicExchange exchange = context.getBean("exchange", TopicExchange.class);
		RabbitTemplate template = context.getBean(RabbitTemplate.class);
		@SuppressWarnings("unchecked")
		List<Queue> queues = context.getBean("queues", List.class);
		for (int i = 0; i < 30; i++) {
			String queue = queues.get(i % queues.size()).getName();
			template.convertAndSend(exchange.getName(), queue, "foo for " + queue);
		}
//		System.in.read();
//		RabbitAdmin admin = context.getBean(RabbitAdmin.class);
//		queues.forEach(q -> admin.deleteQueue(q.getName()));
//		admin.deleteExchange(exchange.getName());
		context.close();
	}

	@Autowired
	private ProducerProperties producerProperties;

	@Bean
	public TopicExchange exchange() {
		return new TopicExchange("foo");
	}

	@Bean
	public List<Queue> queues() {
		List<Queue> queues = new ArrayList<>();
		for (int i = 0; i < this.producerProperties.getPartitionCount(); i++) {
			queues.add(new Queue(this.producerProperties.getDestination() + "-" + i));
		}
		return queues ;
	}

	@Bean
	public List<Binding> fooBinding() {
		List<Binding> bindings = new ArrayList<>();
		for (int i = 0; i < this.producerProperties.getPartitionCount(); i++) {
			bindings.add(BindingBuilder.bind(queues().get(i)).to(exchange()).with(queues().get(i).getName()));
		}
		return bindings;
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
			return new RabbitManagementTemplate(uri, username, password);
		}

	}

}
