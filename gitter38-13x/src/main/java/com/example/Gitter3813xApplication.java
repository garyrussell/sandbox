package com.example;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

@SpringBootApplication
@EnableBinding(Processor.class)
public class Gitter3813xApplication {

	public static void main(String[] args) {
		SpringApplication.run(Gitter3813xApplication.class, args);
	}

	@Bean
	public ApplicationRunner runner(MessageChannel output, Environment environment) {
		return args -> output.send(MessageBuilder.withPayload("foo").setHeader("foo", "bar").build());
	}

	@StreamListener(Processor.INPUT)
	public void listen(Message<?> in) {
		System.out.println(in.getHeaders().get("foo"));
	}

}
