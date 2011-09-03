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
package org.springframework.integration.test.sonar;


import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.Measure;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;

/**
 * @author Gary Russell
 * @since 2.1
 *
 */
public class SensorTests {

	@Test
	public void test() throws Exception {
		Project project = new Project("test");
		ProjectFileSystem pfs = mock(ProjectFileSystem.class);
		SpringIntegrationSensor sensor = new SpringIntegrationSensor(pfs);
		File tmpDir = new File(System.getProperty("java.io.tmpdir"));
		when(pfs.getBuildDir()).thenReturn(tmpDir);
		File tmpFile = new File(tmpDir, "si.coverage");
		FileOutputStream fos = new FileOutputStream(tmpFile);
		fos.write("1.0|2.0|ABC|[x,y,z]\n1.0|3.0|DEF|[1,2,3]\n".getBytes());
		fos.close();
		SensorContext sensorContext = mock(SensorContext.class);
		stub(sensorContext.saveMeasure(any(Measure.class))).toAnswer(
				new Answer<Measure>() {
					public Measure answer(InvocationOnMock invocation)
							throws Throwable {
						Measure measure = (Measure) invocation.getArguments()[0];
						assertEquals("ABC=[x,y,z];DEF=[1,2,3];", measure.getData());
						return measure;
					}
				});
		sensor.analyse(project, sensorContext);
		verify(sensorContext).saveMeasure(SpringIntegrationMetrics.COVERAGE, 40.0);
		tmpFile.delete();
	}

}
