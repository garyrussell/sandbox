package org.foo.stb.redis;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class StbRedisApplication {

	public static void main(String[] args) {
		SpringApplication.run(StbRedisApplication.class, args);
	}

	@Bean
	public IntegrationFlow injestionFlow(ConnectionFactory connectionFactory, StringRedisTemplate redisTemplate) {
		return IntegrationFlows.from(Amqp.inboundAdapter(connectionFactory, "stb"))
				.<String, String>transform(p -> p.replaceAll("<box>[^<]*</box><chan>([^<]*)</chan>", "$1"))
				.handle(redisHandler(redisTemplate))
				.get();
	}

	@Bean
	public MessageHandler redisHandler(final StringRedisTemplate redisTemplate) {
		return new MessageHandler() {

			@Override
			public void handleMessage(final Message<?> message) throws MessagingException {
				final String key = (String) message.getPayload();
				redisTemplate.boundValueOps(key).increment(1);
				System.out.println(key + " is now " + redisTemplate.boundValueOps(key).get());
			}

		};

	}

	@Bean
	public FanoutExchange exchange() {
		return new FanoutExchange("stb");
	}

	@Bean
	public Queue queue() {
		return new Queue("stb");
	}

	@Bean
	public Binding binding() {
		return BindingBuilder.bind(queue()).to(exchange());
	}

}

@RestController
class ChannelCount {

	@Autowired
	private StringRedisTemplate template;

	@RequestMapping("/channel/{channel}")
	public String getCount(@PathVariable String channel) {
		return template.boundValueOps(channel).get() + "\n";
	}

}
