/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.Router;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.redis.store.RedisChannelMessageStore;
import org.springframework.integration.store.MessageGroupQueue;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.Assert;

@SpringBootApplication
public class So34628789Application {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(So34628789Application.class, args);
		MessageChannel foo = context.getBean("foo", MessageChannel.class);
		for (int i = 0; i < 15; i++) {
			foo.send(MessageBuilder.withPayload("foo" + i).build());
		}
		getOutput(context);

		// now with an RLR...

		foo = context.getBean("routedMessageChannel", MessageChannel.class);
		for (int i = 0; i < 15; i++) {
			foo.send(MessageBuilder.withPayload("foo" + (i + 15)).build());
		}
		getOutput(context);

		context.close();
	}

	private static void getOutput(ConfigurableApplicationContext context) {
		int n = 0;
		PollableChannel out = context.getBean("replyQueueChannel", PollableChannel.class);
		for (int i = 0; i < 15; i++) {
			Message<?> received = out.receive(10000);
			if (received != null) {
				System.out.println(received);
				n++;
			}
		}
		out = context.getBean("mailQueueChannel", PollableChannel.class);
		for (int i = 0; i < 15; i++) {
			Message<?> received = out.receive(10000);
			if (received != null) {
				System.out.println(received);
				n++;
			}
		}
		out = context.getBean("auditlogQueueChannel", PollableChannel.class);
		for (int i = 0; i < 15; i++) {
			Message<?> received = out.receive(10000);
			if (received != null) {
				System.out.println(received);
				n++;
			}
		}
		Assert.state(n == 45, "expected 45 messages");
	}

	@Autowired
	private JedisConnectionFactory connectionFactory;

	@Bean
	public MessageChannel replyQueueChannel() {
		return new QueueChannel(new MessageGroupQueue(redisMessageStore(), "replyQueue", 1000));
	}

	@Bean
	public MessageChannel mailQueueChannel() {
		return new QueueChannel(new MessageGroupQueue(redisMessageStore(), "mailQueue", 1000));
	}

	@Bean
	public MessageChannel auditlogQueueChannel() {
		return new QueueChannel(new MessageGroupQueue(redisMessageStore(), "auditLogQueue", 1000));
	}

	@Bean
	public RedisChannelMessageStore redisMessageStore() {
		return new RedisChannelMessageStore(connectionFactory);
	}

	@Bean
	public MessageChannel foo() {
		return new DirectChannel();
	}

	@Bean
	public Foo fooService() {
		return new Foo();
	}

	@Bean
	public MessageChannel routedMessageChannel() {
		return new DirectChannel();
	}

// Router Technique 1
//	@Bean
//	@ServiceActivator(inputChannel="routedMessageChannel")
//	public RecipientListRouter router() {
//		RecipientListRouter router = new RecipientListRouter();
//		List<Recipient> recipients = new ArrayList<>();
//		recipients.add(new Recipient(replyQueueChannel()));
//		recipients.add(new Recipient(mailQueueChannel()));
//		recipients.add(new Recipient(auditlogQueueChannel()));
//		router.setRecipients(recipients);
//		return router;
//	}

	@MessageEndpoint
	public static class Foo {

		@Autowired
		private MessageChannel replyQueueChannel;

		@Autowired
		private MessageChannel mailQueueChannel;

		@Autowired
		private MessageChannel auditlogQueueChannel;

		private List<MessageChannel> channels;

		@ServiceActivator(inputChannel="foo")
		public void sendIt(Message<String> deferredMsg) {
			replyQueueChannel.send(deferredMsg);
			auditlogQueueChannel.send(deferredMsg);
			mailQueueChannel.send (deferredMsg);
		}

		// Router Technique 2
		@Router(inputChannel="routedMessageChannel")
		public List<MessageChannel> route(Message<?> message) {
			if (this.channels == null) {
				this.channels = Arrays.asList(new MessageChannel[] { this.replyQueueChannel, this.mailQueueChannel,
						this.auditlogQueueChannel });
			}
			return this.channels;
		}

	}

}
