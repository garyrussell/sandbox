package org.springframework.integration.test.sonar;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.measures.Metric;
import org.sonar.api.measures.Metrics;

public class SpringIntegrationMetrics implements Metrics {

	/**
	 * 
	 */
	private static final String SPRING_INTEGRATION = "Spring Integration";

	public static final Metric MISSEDCHANNELS = new Metric.Builder("missed_channels",
			"Missed Channels", Metric.ValueType.DATA)
		.setDescription("Channels not covered")
		.setDirection(Metric.DIRECTION_NONE)
		.setQualitative(false)
		.setDomain(SPRING_INTEGRATION)
		.create();

	public static final Metric COVERAGE = new Metric.Builder("channel_coverage", 
			"Channel Coverage", Metric.ValueType.PERCENT)
		.setDescription("Spring Integration Channel Coverage")
		.setDirection(Metric.DIRECTION_BETTER)
		.setQualitative(false)
		.setDomain(SPRING_INTEGRATION)
		.setOptimizedBestValue(true)
		.setBestValue(100.0)
		.create();
	
	public List<Metric> getMetrics() {
		return Arrays.asList(MISSEDCHANNELS, COVERAGE);
	}
}
