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

import java.util.UUID;

import org.springframework.context.SmartLifecycle;
import org.springframework.integration.ip.IpHeaders;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.support.MessageBuilder;
import org.vertx.java.core.Handler;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;

/**
 * @author Gary Russell
 *
 */
public class WebSocketServer extends AbstractServerConnectionFactory implements SmartLifecycle {

	public WebSocketServer(int port) {
		super(port);
	}

	private HttpServer server;

	private volatile boolean running;

	public synchronized void start() {
		if (this.running) {
			return;
		}
		this.server = Vertx.newVertx().createHttpServer()
				.websocketHandler(new Handler<ServerWebSocket>() {
					public void handle(final ServerWebSocket ws) {
						final String correlationId = UUID.randomUUID()
								.toString();
						final TcpListener listener = WebSocketServer.this.getListener();
						WebSocketConnection connection = new WebSocketConnection(
								correlationId, ws, listener);
						WebSocketServer.this.getSender().addNewConnection(connection);
						if (ws.path.equals("/myapp")) {
							ws.dataHandler(new Handler<Buffer>() {
								public void handle(Buffer data) {
									listener.onMessage(MessageBuilder
											.withPayload(data.toString())
											.setCorrelationId(correlationId)
											.setHeader(IpHeaders.CONNECTION_ID, correlationId)
											.build());
								}
							});
						} else {
							ws.reject();
						}
					}
				}).requestHandler(new Handler<HttpServerRequest>() {
					public void handle(HttpServerRequest req) {
						if (req.path.equals("/"))
							req.response.sendFile("ws.html"); // Serve the html
					}
				}).listen(this.getPort());
		this.running = true;
	}

	public void stop() {
		this.running = false;
		this.server.close();
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	@Override
	public int getPhase() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isAutoStartup() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

}
