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

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.Message;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.TcpReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpSendingMessageHandler;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.connection.TcpSender;
import org.springframework.integration.support.MessageBuilder;
import org.vertx.java.core.http.ServerWebSocket;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class WebSocketServerSIBridge {

	private volatile TcpReceivingChannelAdapter adapter;

	private volatile TcpSendingMessageHandler handler;

	public WebSocketServerSIBridge() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext("META-INF/spring/integration/ws-context.xml");
		adapter = ctx.getBean(TcpReceivingChannelAdapter.class);
		handler = ctx.getBean(TcpSendingMessageHandler.class);
	}

	public void doSend(String data, String correlationId) {
		this.adapter.onMessage(MessageBuilder
				.withPayload(data)
				.setCorrelationId(correlationId)
				.setHeader(IpHeaders.CONNECTION_ID, correlationId)
				.build());
	}

	public void registerSocket(ServerWebSocket socket, String correlationId) {
		TcpConnection conn = new WebSocketConnection(correlationId, socket);
		this.handler.addNewConnection(conn);
	}

	private class WebSocketConnection implements TcpConnection {

		private final String id;

		private final ServerWebSocket socket;

		public WebSocketConnection(String id, ServerWebSocket socket) {
			this.id = id;
			this.socket = socket;
		}

		@Override
		public void run() {
		}

		@Override
		public void close() {
		}

		@Override
		public String getConnectionId() {
			return this.id;
		}

		@Override
		public Deserializer<?> getDeserializer() {
			return null;
		}

		@Override
		public String getHostAddress() {
			return "unkown";
		}

		@Override
		public String getHostName() {
			return "unknown";
		}

		@Override
		public TcpListener getListener() {
			return WebSocketServerSIBridge.this.adapter;
		}

		@Override
		public Object getPayload() throws Exception {
			return null;
		}

		@Override
		public int getPort() {
			return 0;
		}

		@Override
		public Serializer<?> getSerializer() {
			return null;
		}

		@Override
		public long incrementAndGetConnectionSequence() {
			return 0;
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public boolean isServer() {
			return false;
		}

		@Override
		public boolean isSingleUse() {
			return false;
		}

		@Override
		public void registerListener(TcpListener arg0) {
		}

		@Override
		public void registerSender(TcpSender arg0) {
		}

		@Override
		public void send(Message<?> message) throws Exception {
			this.socket.writeTextFrame((String) message.getPayload());
		}

		@Override
		public void setDeserializer(Deserializer<?> arg0) {
		}

		@Override
		public void setMapper(TcpMessageMapper arg0) {
		}

		@Override
		public void setSerializer(Serializer<?> arg0) {
		}

		@Override
		public void setSingleUse(boolean arg0) {
		}

	}

	public static class WebSocketServerConnectionFactory extends AbstractServerConnectionFactory {

		public WebSocketServerConnectionFactory() {
			this(0);
		}

		public WebSocketServerConnectionFactory(int port) {
			super(port);
		}

		@Override
		public void run() {
		}

		@Override
		public void close() {
		}


	}

}
