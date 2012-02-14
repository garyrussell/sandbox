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

import org.vertx.java.core.Handler;
import org.vertx.java.core.app.Verticle;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class WebSocketServer implements Verticle {

	private HttpServer server;

	private static final WebSocketServerSIBridge bridge = new WebSocketServerSIBridge();

	public void start() {
		this.server = new HttpServer()
				.websocketHandler(new Handler<ServerWebSocket>() {
					public void handle(final ServerWebSocket ws) {
						final String correlationId = UUID.randomUUID()
								.toString();
						bridge.registerSocket(ws, correlationId);
						if (ws.path.equals("/myapp")) {
							ws.dataHandler(new Handler<Buffer>() {
								public void handle(Buffer data) {
									bridge.doSend(data.toString(), correlationId);
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
				}).listen(8080);
	}

	public void stop() throws Exception {
		this.server.close();
	}

}
