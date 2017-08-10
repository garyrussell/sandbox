package com.example;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;

@SpringBootApplication
public class ConsumerOffsetsBoot15Application implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(ConsumerOffsetsBoot15Application.class, args).close();
	}

	private final ConcurrentMap<Object, Object> offsets = new ConcurrentHashMap<>();

	@KafkaListener(topics = "__consumer_offsets")
	public void listen(ConsumerRecord<?, ?> record) {
		System.out.println(record);
		if (record.value() == null) {
			this.offsets.remove(record.key());
		}
		else {
			this.offsets.put(record.key(), record.value());
		}
	}

	@Autowired
	KafkaTemplate<String, String> template;

	@Override
	public void run(String... arg0) throws Exception {
		template.send("si.topic", "foo");
		Thread.sleep(10_000);
	}

}
