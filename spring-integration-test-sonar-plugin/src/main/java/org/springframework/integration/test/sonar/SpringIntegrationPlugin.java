package org.springframework.integration.test.sonar;

import java.util.Arrays;
import java.util.List;

import org.sonar.api.SonarPlugin;

/**
 * This class is the entry point for all extensions
 */
public class SpringIntegrationPlugin extends SonarPlugin {

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List getExtensions() {
		return Arrays.asList(SpringIntegrationMetrics.class,
				SpringIntegrationSensor.class,
				SpringIntegrationDashboardWidget.class);
	}
}
