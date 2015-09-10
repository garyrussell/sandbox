package org.springframework.amqpclean;

import java.util.List;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.domain.ExchangeInfo;
import com.rabbitmq.http.client.domain.QueueInfo;

@SpringBootApplication
public class AmqpCleanApplication {

	public static void main(String[] args) throws Exception {
//		ConfigurableApplicationContext ctx = SpringApplication.run(AmqpCleanApplication.class, args);
		Client client = new Client("http://localhost:15672/api/", "guest", "guest");
		List<ExchangeInfo> exchanges = client.getExchanges();
		for (ExchangeInfo info : exchanges) {
			if (info.getName().startsWith("xdbus")
					|| (StringUtils.hasText(info.getName()) && !info.getName().startsWith("amq."))) {
				client.deleteExchange("/", info.getName());
			}
		}
		List<QueueInfo> queues = client.getQueues();
		for (QueueInfo info : queues) {
			client.deleteQueue("/", info.getName());
		}
	}

}
