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

import java.util.UUID;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.retry.policy.RetryContextCache;

/**
 * Advice that can be placed in the listener delegate's advice chain to
 * enhance the message with an ID if not present.
 * If an exception is caught, rethrows it as an {@link AmqpRejectAndDontRequeueException}
 * which signals the container to NOT requeue the message (otherwise we'd have infinite
 * immediate retries). If so configured, the broker can send the message to a DLE/DLQ.
 * @author Gary Russell
 *
 */
public class FixMissingMessageIdAdvice implements MethodInterceptor {

	private RetryContextCache retryContextCache;

	public void setRetryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
	}

	public Object invoke(MethodInvocation invocation) throws Throwable {
		String id = null;
		try {
			Message message = (Message) invocation.getArguments()[1];
			MessageProperties messageProperties = message.getMessageProperties();
			if (messageProperties.getMessageId() == null) {
				id = UUID.randomUUID().toString();
				messageProperties.setMessageId(id);
			}
			return invocation.proceed();
		}
		catch (Throwable t) {
			throw new AmqpRejectAndDontRequeueException("Cannot retry message without an ID", t);
		}
		finally {
			if (id != null) {
				retryContextCache.remove(id);
			}
		}
	}

}
