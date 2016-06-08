package com.example.client;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;

import com.example.ExceptionWrapper;
import com.example.service.SomeService;

@SpringBootApplication
@IntegrationComponentScan(basePackageClasses = SomeService.class)
public class ClientApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(ClientApplication.class, args);
		try {
			System.out.println(context.getBean(Client.class).send("FOO"));
			System.out.println(context.getBean(Client.class).send("foo")); // will fail
		}
		finally {
			context.close();
		}
	}

	@Bean
	public Client client() {
		return new Client();
	}

	@Bean
	public IntegrationFlow toAmqpFlow(RabbitTemplate amqpTemplate) {
		return IntegrationFlows.from("toAmqp")
				.handle(Amqp.outboundGateway(amqpTemplate).routingKey("foo"))
				.route(p -> p instanceof ExceptionWrapper,
					m -> m.subFlowMapping("true", sf -> sf.transform("payload.throwable"))
					.resolutionRequired(false)) // good results fall through and are returned
				.get();
	}

	@Bean
	public Queue foo() {
		return new Queue("foo");
	}

}

