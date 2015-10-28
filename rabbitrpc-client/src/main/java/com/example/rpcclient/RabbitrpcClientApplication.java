package com.example.rpcclient;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class RabbitrpcClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(RabbitrpcClientApplication.class, args);
	}

	@Bean
	public DirectExchange exchange() {
		return new DirectExchange("rpc.exchange");
	}

	@Autowired
	private RabbitTemplate rabbitTemplate;

	/*
	 * $ curl http://localhost:8080/torabbit/foo
	 * should return FOO
	 */
	@RequestMapping(value = "/torabbit/{foo}")
	public String handle(@PathVariable("foo") String in) {
		return (String) this.rabbitTemplate.convertSendAndReceive(exchange().getName(), "rpc", in) + "\n";
	}

}
