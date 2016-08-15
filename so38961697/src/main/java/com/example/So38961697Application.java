package com.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

@SpringBootApplication
@EnableBinding(Processor.class)
public class So38961697Application {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(So38961697Application.class, args);
		Foo foo = context.getBean(Foo.class);
		foo.start();
		foo.send();
		Thread.sleep(30000);
		context.close();
	}

	@Bean
	public Foo foo() {
		return new Foo();
	}

	private static class Foo {

		@Autowired
		Processor processor;

		public void send() {
			Message<?> m = MessageBuilder.withPayload("foo")
					.setHeader("bar", "baz")
					.build();
			processor.output().send(m);
		}

		public void start() {
			this.processor.input().subscribe(new MessageHandler() {

				@Override
				public void handleMessage(Message<?> m) throws MessagingException {
					System.out.println(m);
				}

			});
		}

	}

}
