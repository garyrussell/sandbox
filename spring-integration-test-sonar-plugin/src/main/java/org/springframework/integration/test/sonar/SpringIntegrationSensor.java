package org.springframework.integration.test.sonar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

public class SpringIntegrationSensor implements Sensor {

	private ProjectFileSystem projectFileSystem;

	public SpringIntegrationSensor(ProjectFileSystem pfs) {
		this.projectFileSystem = pfs;
	}
	
	public boolean shouldExecuteOnProject(Project project) {
		// this sensor is executed on any type of project
		return true;
	}

	public void analyse(Project project, SensorContext sensorContext) {
		double covered = 0;
		double channelCount = 0;
		File coverageFile = new File(this.projectFileSystem.getBuildDir(), "si.coverage");
		Map<String, String> uncovered = new HashMap<String, String>();
		if (coverageFile.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(coverageFile));
				String line;
				while ((line = br.readLine()) != null) {
					String[] splits = line.split("\\|");
					float cov = Float.parseFloat(splits[0]);
					float cnt = Float.parseFloat(splits[1]);
					covered += cov;
					channelCount += cnt;
					if (cov < cnt) {
						uncovered.put(splits[2], splits[3]);
					}
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		} else {
			return;
		}
		sensorContext.saveMeasure(SpringIntegrationMetrics.COVERAGE, covered / channelCount * 100.0);
		StringBuilder sb = new StringBuilder();
		for (String key : uncovered.keySet()) {
			sb.append(key).append("=").append(uncovered.get(key)).append(";");
		}
		Measure measure = new Measure(SpringIntegrationMetrics.MISSEDCHANNELS, sb.toString());
		sensorContext.saveMeasure(measure);
	}

	public String toString() {
		return "Spring Integration Channel Coverage";
	}
}
