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

package com.example.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.messaging.MessagingException;

import com.example.ExceptionWrapper;
import com.example.service.SomeServiceImpl;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(ServerApplication.class, args);
		Thread.sleep(10000);
		context.close();
	}

	@Bean
	public IntegrationFlow fromAmqp(ConnectionFactory connectionFactory) {
		return IntegrationFlows.from(Amqp.inboundGateway(connectionFactory, "foo").errorChannel("errors"))
				.enrichHeaders(h -> h.headerExpression("bar", "payload[1]"))
				.transform("payload[0]")
				.handle("service", "process")
				.get();
	}

	@Bean
	public SomeServiceImpl service() {
		return new SomeServiceImpl();
	}

	@Bean
	public ErrorHandler errorHandler() {
		return new ErrorHandler();
	}

	@MessageEndpoint
	public static class ErrorHandler {

		private final Log logger = LogFactory.getLog(this.getClass());

		@ServiceActivator(inputChannel = "errors")
		public ExceptionWrapper exception(MessagingException e) {
			this.logger.error("Failed: " + e.getFailedMessage());
			return new ExceptionWrapper(e.getCause());
		}

	}

}
