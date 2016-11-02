/*
 * Copyright 2012-2015 the original author or authors.
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

package com.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.integration.support.management.Statistics;
import org.springframework.lang.UsesJava7;

/**
 * A {@link MetricReader} for Spring Integration metrics (as provided by
 * spring-integration-jmx).
 *
 * @author Dave Syer
 * @author Gary Russell
 * @since 1.3.0
 */
@UsesJava7
public class SpringIntegrationMetricReader implements MetricReader, EnvironmentAware {

	private final IntegrationMBeanExporter exporter;

	private String appPrefix = "integration";

	public SpringIntegrationMetricReader(IntegrationMBeanExporter exporter) {
		this.exporter = exporter;
	}

	@Override
	public void setEnvironment(Environment environment) {
		String streamName = environment.getProperty("stream.name");
		String appLabel = environment.getProperty("app.label");
		if (streamName != null) {
			this.appPrefix = streamName + "." + appLabel;
		}
	}

	@Override
	public Metric<?> findOne(String metricName) {
		return null;
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		IntegrationMBeanExporter exporter = this.exporter;
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		for (String name : exporter.getChannelNames()) {
			String prefix = this.appPrefix + ".channel." + name;
			metrics.addAll(getStatistics(prefix + ".errorRate",
					exporter.getChannelErrorRate(name)));
			metrics.add(new Metric<Long>(prefix + ".sendCount",
					exporter.getChannelSendCountLong(name)));
			metrics.addAll(getStatistics(prefix + ".sendRate",
					exporter.getChannelSendRate(name)));
			metrics.add(new Metric<Long>(prefix + ".receiveCount",
					exporter.getChannelReceiveCountLong(name)));
		}
		for (String name : exporter.getHandlerNames()) {
			metrics.addAll(getStatistics(this.appPrefix + ".handler." + name + ".duration",
					exporter.getHandlerDuration(name)));
		}
		metrics.add(new Metric<Integer>(this.appPrefix + ".activeHandlerCount",
				exporter.getActiveHandlerCount()));
		metrics.add(new Metric<Integer>(this.appPrefix + ".handlerCount",
				exporter.getHandlerCount()));
		metrics.add(new Metric<Integer>(this.appPrefix + ".channelCount",
				exporter.getChannelCount()));
		metrics.add(new Metric<Integer>(this.appPrefix + ".queuedMessageCount",
				exporter.getQueuedMessageCount()));
		return metrics;
	}

	private Collection<? extends Metric<?>> getStatistics(String name,
			Statistics statistic) {
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		metrics.add(new Metric<Double>(name + ".mean", statistic.getMean()));
		metrics.add(new Metric<Double>(name + ".max", statistic.getMax()));
		metrics.add(new Metric<Double>(name + ".min", statistic.getMin()));
		metrics.add(
				new Metric<Double>(name + ".stdev", statistic.getStandardDeviation()));
		metrics.add(new Metric<Long>(name + ".count", statistic.getCountLong()));
		return metrics;
	}

	@Override
	public long count() {
		int totalChannelCount = this.exporter.getChannelCount() * 11;
		int totalHandlerCount = this.exporter.getHandlerCount() * 5;
		return totalChannelCount + totalHandlerCount + 4;
	}

}
