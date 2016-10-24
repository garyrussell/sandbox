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

package org.springframework.amqp.producer;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
@ConfigurationProperties(prefix = "producer")
public class ProducerProperties {

	/**
	 * The destination.
	 */
	private String destination;

	/**
	 * Number of partitions to publish to; a queue will be created for each.
	 */
	private int partitionCount;

	public String getDestination() {
		return this.destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public int getPartitionCount() {
		return this.partitionCount;
	}

	public void setPartitionCount(int partitionCount) {
		this.partitionCount = partitionCount;
	}

}
