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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class TcpSSLServerConnectionFactory extends
		TcpNetServerConnectionFactory {

	/**
	 * @param port
	 */
	public TcpSSLServerConnectionFactory(int port) {
		super(port);
	}

	@Override
	protected ServerSocket createServerSocket(int port, int backlog,
			InetAddress whichNic) throws IOException {
		SSLServerSocket sss = (SSLServerSocket) SSLServerSocketFactory.getDefault()
				.createServerSocket(this.getPort(), backlog, whichNic);
		return sss;
	}

}
