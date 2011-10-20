package org.springframework.integration.ip.addons;

import java.io.IOException;
import java.net.Socket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;

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

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class TcpSSLClientConnectionFactory extends
		TcpNetClientConnectionFactory {

	public TcpSSLClientConnectionFactory(String host, int port) {
		super(host, port);
	}

	@Override
	protected Socket createSocket(String host, int port) throws IOException {
		SSLSocket ss = (SSLSocket) SSLSocketFactory.getDefault().createSocket(
				this.getHost(), this.getPort());
		return ss;
	}


}
