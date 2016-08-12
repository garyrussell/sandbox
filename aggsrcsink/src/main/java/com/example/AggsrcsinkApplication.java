package com.example;

import org.springframework.cloud.stream.aggregate.AggregateApplicationBuilder;
import org.springframework.cloud.stream.app.log.sink.rabbit.LogSinkRabbitApplication;
import org.springframework.cloud.stream.app.sftp.source.rabbit.SftpSourceRabbitApplication;
import org.springframework.context.ConfigurableApplicationContext;

//@SpringBootApplication
public class AggsrcsinkApplication {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = new AggregateApplicationBuilder()
			.from(SftpSourceRabbitApplication.class)
			.to(LogSinkRabbitApplication.class)
			.run();
		Thread.sleep(30000);
		context.close();
	}

}
