package com.example;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.protocol.types.Type;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ConsumerSeekAware;

@SpringBootApplication
public class ConsumerOffsetsApplication implements CommandLineRunner, ConsumerSeekAware {

	public static void main(String[] args) {
		SpringApplication.run(ConsumerOffsetsApplication.class, args).close();
	}

	private final ConcurrentMap<String, Long> offsets = new ConcurrentHashMap<>();

	@Override
	public void registerSeekCallback(ConsumerSeekCallback callback) {
	}

	@Override
	public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
		assignments.forEach((k, v) -> callback.seekToBeginning(k.topic(), k.partition()));
	}

	@Override
	public void onIdleContainer(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
	}

	@KafkaListener(topics = "__consumer_offsets", groupId = "offsetMon1")
	public void listen(ConsumerRecord<byte[], byte[]> record) {
		try {
			ByteBuffer bb = ByteBuffer.wrap(record.key());
			bb.getShort();
			String group = (String) Type.STRING.read(bb);
			String topic = (String) Type.STRING.read(bb);
			int partition = (int) Type.INT32.read(bb);
			String key = group + ":" + topic + ":" + partition;
			bb = ByteBuffer.wrap(record.value());
			bb.getShort();
			long offset = (long) Type.INT64.read(bb);
//			System.out.println(key + " @ " + offset);
			if (record.value() == null) {
				this.offsets.remove(key);
			}
			else {
				this.offsets.put(key, offset);
			}
		}
		catch (Exception e) {
//			e.printStackTrace();
		}
	}

	@Autowired
	KafkaTemplate<String, String> template;

	@Override
	public void run(String... arg0) throws Exception {
		template.send("si.topic", "foo");
		Thread.sleep(20_000);
		System.out.println(this.offsets);
	}

}
