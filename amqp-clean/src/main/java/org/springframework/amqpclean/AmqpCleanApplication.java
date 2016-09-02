package org.springframework.amqpclean;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

import com.rabbitmq.http.client.Client;

@SpringBootApplication
public class AmqpCleanApplication {

	public static void main(String[] args) throws Exception {
		Client client = new Client("http://localhost:15672/api/", "guest", "guest");
		client.getExchanges()
				.stream()
				.filter(ei -> StringUtils.hasText(ei.getName()) && !ei.getName().startsWith("amq."))
				.forEach(ei -> client.deleteExchange("/", ei.getName()));
		client.getQueues()
				.stream()
				.forEach(qi -> client.deleteQueue("/", qi.getName()));
	}

}
