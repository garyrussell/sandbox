package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.core.AnonymousQueue;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;

@SpringBootApplication
@EnableBinding(Processor.class)
public class ScsFileRabbit1Application {

	public static void main(String[] args) {
		SpringApplication.run(ScsFileRabbit1Application.class, args);
	}

	@Bean
	public FileToRabbit fileToRabbit() {
		return new FileToRabbit();
	}

	public static class FileToRabbit {

		private static final Log logger = LogFactory.getLog(FileToRabbit.class);

		@Autowired
		private ApplicationContext context;

		@ServiceActivator(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
		public Map<String, String> process(Object input) {
			File file = null;
			if (input instanceof File) {
				file = (File) input;
			}
			else if (input instanceof String) {
				file = new File((String) input);
			}
			else {
				logger.error("Unsupported inbound payload " + input.getClass());
				return null;
			}
			RabbitAdmin admin = this.context.getBean(RabbitAdmin.class); // need to figure out why @Autowired doesn't work
			String queueName = new AnonymousQueue.Base64UrlNamingStrategy().generateName();
			Queue queue = new Queue(queueName, true, false, false);
			admin.declareQueue(queue);
			try {
				BufferedReader reader = new BufferedReader(new FileReader(file));
				RabbitTemplate template = context.getBean(RabbitTemplate.class);
				String line;
				while ((line = reader.readLine()) != null) {
					template.convertAndSend(queueName, line);
				}
				reader.close();
			}
			catch (IOException e) {
				logger.error("Error processing input " + input, e);
				admin.deleteQueue(queueName);
				return null;
			}
			return Collections.singletonMap("queueName", queueName);
		}

	}

}
