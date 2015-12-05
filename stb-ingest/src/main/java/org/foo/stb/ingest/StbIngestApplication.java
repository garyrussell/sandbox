package org.foo.stb.ingest;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.amqp.Amqp;
import org.springframework.integration.dsl.http.Http;

@SpringBootApplication
public class StbIngestApplication {

	public static void main(String[] args) {
		SpringApplication.run(StbIngestApplication.class, args);
	}

	@Bean
	public IntegrationFlow ingestionFlow(RabbitTemplate rabbitTemplate) {
		return IntegrationFlows.from(Http.inboundChannelAdapter("/ingest")
					.requestMapping(m -> m.methods(HttpMethod.POST))
					.requestPayloadType(String.class))
				.handle(Amqp.outboundAdapter(rabbitTemplate)
						.defaultDeliveryMode(MessageDeliveryMode.NON_PERSISTENT)
						.exchangeName(exchange().getName())
						.routingKey("stb"))
				.get();
	}

	@Bean
	public Exchange exchange() {
		return new FanoutExchange("stb");
	}

}
