package org.springframework.amqp.protonj;

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
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class RepartitionApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(RepartitionApplication.class, args);
		TopicExchange exchange = context.getBean("foo", TopicExchange.class);
		RabbitAdmin admin = context.getBean(RabbitAdmin.class);
		admin.getQueueProperties("foo-0"); // kick start - creates a connection
		RabbitManagementTemplate rmt = new RabbitManagementTemplate();
		Optional<Integer> max = maxConsumerPartition(rmt);
		while (!max.isPresent()) {
			Thread.sleep(1000);
			max = maxConsumerPartition(rmt);
		}
		int currentMax = max.get();
		boolean stop = false;
		boolean rebalancedUp = false;
		while (!stop) {
			Thread.sleep(10000);
			int newMax = maxConsumerPartition(rmt).get();
			System.out.println("New high partition: " + newMax);
			if (newMax > currentMax) {
				System.out.println("Partitions increased to " + newMax + ", rebalancing...");
			}
			else if (newMax < currentMax) {
				System.out.println("Partitions decreased to " + newMax + ", rebalancing...");
				stop = true;
			}
			if (newMax == 0 && !rebalancedUp) {
				// simulate a new instance starting up
				Queue foo1 = new Queue("foo-1");
				admin.declareQueue(foo1);
				admin.declareBinding(BindingBuilder.bind(foo1).to(exchange).with("foo-1"));
				rebalancedUp = true;
			}
			else {
				// simulate the highest instance going away
				admin.deleteQueue("foo-1");
			}
			currentMax = newMax;
		}
		admin.deleteQueue("foo-0");
		admin.deleteExchange("foo");
		context.close();
	}

	private static Optional<Integer> maxConsumerPartition(RabbitManagementTemplate rmt) {
		return rmt.getBindingsForExchange("/", "foo")
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

}
