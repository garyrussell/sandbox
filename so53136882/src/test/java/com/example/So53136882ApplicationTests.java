package com.example;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;

import com.rabbitmq.client.Channel;

@RunWith(SpringRunner.class)
public class So53136882ApplicationTests {

	@Autowired
	private RabbitListenerEndpointRegistry registry;

	@Autowired
	private RabbitOperations rabbitTemplate;

	@Test
	public void test() throws Exception {
		SimpleMessageListenerContainer container = (SimpleMessageListenerContainer) this.registry
				.getListenerContainer("myListener");
		ChannelAwareMessageListener listener = (ChannelAwareMessageListener) container.getMessageListener();
		Message message = MessageBuilder.withBody("{\"bar\":\"baz\"}".getBytes())
				.andProperties(MessagePropertiesBuilder.newInstance()
						.setContentType("application/json")
						.build())
				.build();
		listener.onMessage(message, mock(Channel.class));
		verify(this.rabbitTemplate).convertAndSend("someExchange", "someRoutingKey", new Foo("BAZ"));
	}

	@Configuration
	@EnableRabbit
	public static class config {

		@Bean
		public ConnectionFactory mockCf() {
			return mock(ConnectionFactory.class);
		}

		@Bean
		public MessageConverter converter() {
			return new Jackson2JsonMessageConverter();
		}

		@Bean
		public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory() {
			SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
			factory.setConnectionFactory(mockCf());
			factory.setMessageConverter(converter());
			factory.setAutoStartup(false);
			return factory;
		}

		@Bean
		public MyListener myListener() {
			return new MyListener();
		}

		@Bean
		public SomeService service() {
			return new SomeServiceImpl();
		}

		@Bean
		public RabbitOperations rabbitTemplate() {
			return mock(RabbitOperations.class);
		}

	}

}
