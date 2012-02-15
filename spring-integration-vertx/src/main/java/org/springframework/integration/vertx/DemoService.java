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
package org.springframework.integration.vertx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class DemoService {

	private static final Log logger = LogFactory.getLog(DemoService.class);

	private final Map<String, AtomicInteger> clients = new HashMap<>();

	public void startStop(String command, @Header(IpHeaders.CONNECTION_ID) String connectionId) {
		if ("stop".equalsIgnoreCase(command)) {
			clients.remove(connectionId);
			logger.info("Connection " + connectionId + " stopped");
		}
		else if ("start".equalsIgnoreCase(command)) {
			clients.put(connectionId, new AtomicInteger());
			logger.info("Connection " + connectionId + " started");
		}
	}

	public List<Message<?>> getNext() {
		List<Message<?>> messages = new ArrayList<>();
		for (Entry<String, AtomicInteger> entry : clients.entrySet()) {
			Message<String> message = MessageBuilder.withPayload(Integer.toString(entry.getValue().incrementAndGet()))
					.setHeader(IpHeaders.CONNECTION_ID, entry.getKey())
					.build();
			messages.add(message);
			logger.info("Sending " + message.getPayload() + " to connection " + entry.getKey());
		}
		return messages;
	}

}
