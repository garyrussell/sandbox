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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.MessagingException;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractTcpConnection;
import org.springframework.integration.ip.tcp.connection.AbstractTcpConnectionInterceptor;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpListener;
import org.springframework.integration.ip.tcp.connection.TcpMessageMapper;
import org.springframework.integration.ip.tcp.connection.TcpSender;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class CachingTcpConnectionFactory extends AbstractClientConnectionFactory {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final AbstractClientConnectionFactory targetConnectionFactory;

	private volatile int poolSize = 2;

	private BlockingQueue<TcpConnection> available = new LinkedBlockingQueue<TcpConnection>();

	private Set<TcpConnection> inUse = new HashSet<TcpConnection>();

	private volatile int availableTimeout = -1;

	public CachingTcpConnectionFactory(AbstractClientConnectionFactory target) {
		super("", 0);
		// override single-use to true to force "close" after use
		target.setSingleUse(true);
		this.targetConnectionFactory = target;
	}

	public TcpConnection getOrMakeConnection() throws Exception {
		TcpConnection connection = null;
		connection = getConnectionNoWait(connection);
		if (connection == null) {
			long now = System.currentTimeMillis();
			int timeout = this.availableTimeout;
			while (connection == null) {
				if (timeout > 0) {
					if (logger.isDebugEnabled()) {
						logger.debug("Waiting for connection to become available");
					}
					connection = retrieveConnection(timeout);
				}
				if (connection == null) {
					connection = getConnectionNoWait(connection);
				}
				timeout -= (System.currentTimeMillis() - now);
				if (connection == null && timeout <= 0) {
					throw new MessagingException("No connections available");
				}
			}
		}
		return connection;
	}

	protected TcpConnection getConnectionNoWait(TcpConnection connection)
			throws Exception {
		synchronized (this.targetConnectionFactory) {
			while (connection == null && this.available.size() > 0) {
				connection = retrieveConnection(-1);
			}
			if (connection == null) {
				if (this.getAllocated() < this.poolSize) {
					connection = newConnection();
					this.inUse.add(connection);
					if (logger.isDebugEnabled()) {
						logger.debug("Created new connection " + connection.getConnectionId());
					}
				}
			}
		}
		return connection;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	protected TcpConnection newConnection() throws Exception {
		TcpConnection connection = this.targetConnectionFactory.getConnection();
		return new CachedConnection(connection);

	}

	protected TcpConnection retrieveConnection(int timeout) throws Exception {
		TcpConnection connection;
		if (timeout > 0) {
			connection = this.available.poll(timeout,
				TimeUnit.MILLISECONDS);
		} else {
			connection = this.available.poll();
		}
		if (connection == null) {
			return null;
		}
		if (!connection.isOpen()) {
			if (logger.isDebugEnabled()) {
				logger.debug(connection.getConnectionId() + " is closed, trying another");
			}
			return null;
		}
		synchronized (this.targetConnectionFactory) {
			this.inUse.add(connection);
		}
		if (logger.isDebugEnabled() && connection != null) {
			logger.debug("Retrieved " + connection.getConnectionId()
					+ " from cache");
		}
		return connection;
	}


	protected int getAllocated() {
		synchronized (this.targetConnectionFactory) {
			return this.available.size() + this.inUse.size();
		}
	}

	/**
	 * @return the poolSize
	 */
	public int getPoolSize() {
		return poolSize;
	}

	/**
	 * @param poolSize the poolSize to set
	 */
	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

	public void run() {

	}

	public boolean isRunning() {
		return targetConnectionFactory.isRunning();
	}

	@Override
	public void close() {
		targetConnectionFactory.close();
	}



	/**
	 * @param availableTimeout the availableTimeout to set
	 */
	public void setAvailableTimeout(int availableTimeout) {
		this.availableTimeout = availableTimeout;
	}



	private class CachedConnection extends AbstractTcpConnectionInterceptor {

		public CachedConnection(TcpConnection connection) {
			super.setTheConnection(connection);
			if (connection instanceof AbstractTcpConnection) {
				((AbstractTcpConnection) connection).registerListener(this);
			}
		}


		@Override
		public void close() {
			/**
			 * If the delegate is stopped, actually close
			 * the connection.
			 */
			if (!isRunning()) {
				super.close();
				return;
			}
			synchronized (targetConnectionFactory) {
				inUse.remove(this);
				available.add(this);
				if (logger.isDebugEnabled()) {
					logger.debug("Returned " + this.getConnectionId() + " to cache");
				}
			}
		}


		@Override
		public String getConnectionId() {
			return "Cached:" + super.getConnectionId();
		}


		@Override
		public String toString() {
			return this.getConnectionId();
		}


	}

///////////////// DELEGATE METHODS ///////////////////////

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return targetConnectionFactory.hashCode();
	}

	/**
	 * @param componentName
	 * @see org.springframework.integration.context.IntegrationObjectSupport#setComponentName(java.lang.String)
	 */
	public void setComponentName(String componentName) {
		targetConnectionFactory.setComponentName(componentName);
	}

	/**
	 * @return
	 * @see org.springframework.integration.context.IntegrationObjectSupport#getComponentType()
	 */
	public String getComponentType() {
		return targetConnectionFactory.getComponentType();
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return targetConnectionFactory.equals(obj);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getSoTimeout()
	 */
	public int getSoTimeout() {
		return targetConnectionFactory.getSoTimeout();
	}

	/**
	 * @param soTimeout
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoTimeout(int)
	 */
	public void setSoTimeout(int soTimeout) {
		targetConnectionFactory.setSoTimeout(soTimeout);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getSoReceiveBufferSize()
	 */
	public int getSoReceiveBufferSize() {
		return targetConnectionFactory.getSoReceiveBufferSize();
	}

	/**
	 * @param soReceiveBufferSize
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoReceiveBufferSize(int)
	 */
	public void setSoReceiveBufferSize(int soReceiveBufferSize) {
		targetConnectionFactory.setSoReceiveBufferSize(soReceiveBufferSize);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getSoSendBufferSize()
	 */
	public int getSoSendBufferSize() {
		return targetConnectionFactory.getSoSendBufferSize();
	}

	/**
	 * @param soSendBufferSize
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoSendBufferSize(int)
	 */
	public void setSoSendBufferSize(int soSendBufferSize) {
		targetConnectionFactory.setSoSendBufferSize(soSendBufferSize);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#isSoTcpNoDelay()
	 */
	public boolean isSoTcpNoDelay() {
		return targetConnectionFactory.isSoTcpNoDelay();
	}

	/**
	 * @param soTcpNoDelay
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoTcpNoDelay(boolean)
	 */
	public void setSoTcpNoDelay(boolean soTcpNoDelay) {
		targetConnectionFactory.setSoTcpNoDelay(soTcpNoDelay);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getSoLinger()
	 */
	public int getSoLinger() {
		return targetConnectionFactory.getSoLinger();
	}

	/**
	 * @param soLinger
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoLinger(int)
	 */
	public void setSoLinger(int soLinger) {
		targetConnectionFactory.setSoLinger(soLinger);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#isSoKeepAlive()
	 */
	public boolean isSoKeepAlive() {
		return targetConnectionFactory.isSoKeepAlive();
	}

	/**
	 * @param soKeepAlive
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoKeepAlive(boolean)
	 */
	public void setSoKeepAlive(boolean soKeepAlive) {
		targetConnectionFactory.setSoKeepAlive(soKeepAlive);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getSoTrafficClass()
	 */
	public int getSoTrafficClass() {
		return targetConnectionFactory.getSoTrafficClass();
	}

	/**
	 * @param soTrafficClass
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSoTrafficClass(int)
	 */
	public void setSoTrafficClass(int soTrafficClass) {
		targetConnectionFactory.setSoTrafficClass(soTrafficClass);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getHost()
	 */
	public String getHost() {
		return targetConnectionFactory.getHost();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getPort()
	 */
	public int getPort() {
		return targetConnectionFactory.getPort();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getListener()
	 */
	public TcpListener getListener() {
		return targetConnectionFactory.getListener();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getSender()
	 */
	public TcpSender getSender() {
		return targetConnectionFactory.getSender();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getSerializer()
	 */
	public Serializer<?> getSerializer() {
		return targetConnectionFactory.getSerializer();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getDeserializer()
	 */
	public Deserializer<?> getDeserializer() {
		return targetConnectionFactory.getDeserializer();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getMapper()
	 */
	public TcpMessageMapper getMapper() {
		return targetConnectionFactory.getMapper();
	}

	/**
	 * @param listener
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#registerListener(org.springframework.integration.ip.tcp.connection.TcpListener)
	 */
	public void registerListener(TcpListener listener) {
		targetConnectionFactory.registerListener(listener);
	}

	/**
	 * @param sender
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#registerSender(org.springframework.integration.ip.tcp.connection.TcpSender)
	 */
	public void registerSender(TcpSender sender) {
		targetConnectionFactory.registerSender(sender);
	}

	/**
	 * @param taskExecutor
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setTaskExecutor(java.util.concurrent.Executor)
	 */
	public void setTaskExecutor(Executor taskExecutor) {
		targetConnectionFactory.setTaskExecutor(taskExecutor);
	}

	/**
	 * @param deserializer
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setDeserializer(org.springframework.core.serializer.Deserializer)
	 */
	public void setDeserializer(Deserializer<?> deserializer) {
		targetConnectionFactory.setDeserializer(deserializer);
	}

	/**
	 * @param serializer
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSerializer(org.springframework.core.serializer.Serializer)
	 */
	public void setSerializer(Serializer<?> serializer) {
		targetConnectionFactory.setSerializer(serializer);
	}

	/**
	 * @param mapper
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setMapper(org.springframework.integration.ip.tcp.connection.TcpMessageMapper)
	 */
	public void setMapper(TcpMessageMapper mapper) {
		targetConnectionFactory.setMapper(mapper);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#isSingleUse()
	 */
	public boolean isSingleUse() {
		return targetConnectionFactory.isSingleUse();
	}

	/**
	 * @param singleUse
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setSingleUse(boolean)
	 */
	public void setSingleUse(boolean singleUse) {
		targetConnectionFactory.setSingleUse(singleUse);
	}

	/**
	 * @param interceptorFactoryChain
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setInterceptorFactoryChain(org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain)
	 */
	public void setInterceptorFactoryChain(
			TcpConnectionInterceptorFactoryChain interceptorFactoryChain) {
		targetConnectionFactory
				.setInterceptorFactoryChain(interceptorFactoryChain);
	}

	/**
	 * @param lookupHost
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#setLookupHost(boolean)
	 */
	public void setLookupHost(boolean lookupHost) {
		targetConnectionFactory.setLookupHost(lookupHost);
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#isLookupHost()
	 */
	public boolean isLookupHost() {
		return targetConnectionFactory.isLookupHost();
	}

	/**
	 *
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#start()
	 */
	public void start() {
		this.setActive(true);
		targetConnectionFactory.start();
	}

	/**
	 *
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#stop()
	 */
	public void stop() {
		targetConnectionFactory.stop();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#getPhase()
	 */
	public int getPhase() {
		return targetConnectionFactory.getPhase();
	}

	/**
	 * @return
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#isAutoStartup()
	 */
	public boolean isAutoStartup() {
		return targetConnectionFactory.isAutoStartup();
	}

	/**
	 * @param callback
	 * @see org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory#stop(java.lang.Runnable)
	 */
	public void stop(Runnable callback) {
		targetConnectionFactory.stop(callback);
	}

}
