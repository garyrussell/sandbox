/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.ip.addons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.message.GenericMessage;

/**
 * SSL Tests.
 *
 * Run with<br/>
 * -Djavax.net.ssl.keyStore=target/classes/test.ks -Djavax.net.ssl.keyStorePassword=secret<br/>
 * -Djavax.net.ssl.trustStore=target/classes/truststore -Djavax.net.ssl.trustStorePassword=secret<br/>
 * (with proper location/passwords for your keystore and truststore).
 *
 * @author Gary Russell
 * @since 2.1
 *
 */
public class SSLTests {

	@Test
	public void testAdapters() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"SSLTests-adapters-context.xml", this.getClass());

		waitForServer(ctx);

		MessageChannel in = ctx.getBean("in", MessageChannel.class);
		SubscribableChannel out = ctx.getBean("out", SubscribableChannel.class);
		final CountDownLatch latch = new CountDownLatch(1);
		out.subscribe(new MessageHandler() {

			public void handleMessage(Message<?> message)
					throws MessagingException {
				@SuppressWarnings("unchecked")
				Message<byte[]> byteArrayMessage = (Message<byte[]>) message;
				System.out.println(new String(byteArrayMessage.getPayload()));
				latch.countDown();
			}});

		in.send(new GenericMessage<String>("Hello, world!"));
		assertTrue(latch.await(10,TimeUnit.SECONDS));
	}

	/**
	 * @param ctx
	 * @throws InterruptedException
	 */
	protected void waitForServer(ApplicationContext ctx)
			throws InterruptedException {
		TcpSSLServerConnectionFactory scf = ctx.getBean(TcpSSLServerConnectionFactory.class);
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			n += 100;
			if (n > 10000) {
				fail("Server failed to start listening");
			}
		}
	}

	@Test
	public void testGateways() throws Exception {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"SSLTests-gateways-context.xml", this.getClass());

		waitForServer(ctx);

		MessageChannel in = ctx.getBean("in", MessageChannel.class);
		SubscribableChannel out = ctx.getBean("out", SubscribableChannel.class);
		final CountDownLatch latch = new CountDownLatch(1);
		out.subscribe(new MessageHandler() {

			public void handleMessage(Message<?> message)
					throws MessagingException {
				@SuppressWarnings("unchecked")
				Message<byte[]> byteArrayMessage = (Message<byte[]>) message;
				String payload = new String(byteArrayMessage.getPayload());
				System.out.println(payload);
				assertEquals("echo:Hello, world!", payload);
				latch.countDown();
			}});

		in.send(new GenericMessage<String>("Hello, world!"));
		assertTrue(latch.await(10,TimeUnit.SECONDS));
	}

}
