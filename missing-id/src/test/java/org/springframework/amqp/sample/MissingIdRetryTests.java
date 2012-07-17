/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.amqp.sample;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.aopalliance.aop.Advice;
import org.junit.Test;
import org.springframework.amqp.rabbit.config.StatefulRetryOperationsInterceptorFactoryBean;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.retry.policy.MapRetryContextCache;
import org.springframework.retry.policy.RetryContextCache;
import org.springframework.retry.support.RetryTemplate;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class MissingIdRetryTests {

	private CountDownLatch latch = new CountDownLatch(1);

	@Test
	public void test() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("rabbit-context.xml", this.getClass());
		RabbitTemplate template = ctx.getBean(RabbitTemplate.class);
		ConnectionFactory connectionFactory = ctx.getBean(ConnectionFactory.class);
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
		container.setMessageListener(new MessageListenerAdapter(new POJO()));
		container.setQueueNames("si.test.queue");

		StatefulRetryOperationsInterceptorFactoryBean fb = new StatefulRetryOperationsInterceptorFactoryBean();

		// use an external template so we can share his cache
		RetryTemplate retryTemplate = new RetryTemplate();
		RetryContextCache cache = new MapRetryContextCache();
		retryTemplate.setRetryContextCache(cache);
		fb.setRetryOperations(retryTemplate);

		FixMissingMessageIdAdvice missingIdAdvice = new FixMissingMessageIdAdvice();
		// give him a reference to the retry cache so he can clean it up
		missingIdAdvice.setRetryContextCache(cache);

		Advice retryInterceptor = fb.getObject();
		// add both advices
		container.setAdviceChain(new Advice[] {missingIdAdvice, retryInterceptor});
		container.start();

		template.convertAndSend("si.test.exchange", "si.test.binding", "Hello, world!");
		assertTrue(latch.await(10, TimeUnit.SECONDS));
		Thread.sleep(10000);
	}

	public class POJO {
		public void handleMessage(String foo) {
			System.out.println(foo);
			latch.countDown();
			throw new RuntimeException("fail");
		}
	}
}
