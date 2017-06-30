package com.example.seek;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;

/**
 * Run this before the Scst app
 * @author Gary Russell
 */
@SpringBootApplication
@EnableBinding(Sink.class)
public class KperfSeekZeroApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(KperfSeekZeroApplication.class, args).close();
	}

	private final CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void run(String... arg0) throws Exception {
		latch.await();
		Thread.sleep(10_000);
	}

	@Bean
	public KafkaMessageListenerContainer<?, ?> container(ConsumerFactory<?, ?> consumerFactory) {
		ContainerProperties props = new ContainerProperties("perf");
		Map<String, Object> configs = new HashMap<>(
				((DefaultKafkaConsumerFactory<?, ?>) consumerFactory).getConfigurationProperties());
		configs.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, 2 * 1024 * 1024);
		configs.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024);
		configs.put(ConsumerConfig.CHECK_CRCS_CONFIG, false);
		props.setPollTimeout(100);
		props.setConsumerRebalanceListener(new RebalanceListener());
		Listener messageListener = new Listener();
		props.setMessageListener(messageListener);
		KafkaMessageListenerContainer<Object, Object> container = new KafkaMessageListenerContainer<>(
				new DefaultKafkaConsumerFactory<>(configs), props);
		messageListener.setContainer(container);
		return container;
	}

	public class Listener implements MessageListener<byte[], byte[]> {

		private volatile KafkaMessageListenerContainer<?, ?> container;

		public void setContainer(KafkaMessageListenerContainer<?, ?> container) {
			this.container = container;
		}


		@Override
		public void onMessage(ConsumerRecord<byte[], byte[]> record) {
			Executors.newSingleThreadExecutor().execute(() -> this.container.stop());
			latch.countDown();
		}

	}

	public static class RebalanceListener implements ConsumerAwareRebalanceListener {

		private static final Log logger = LogFactory.getLog(RebalanceListener.class);

		@Override
		public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> parts) {
			TopicPartition tp = new TopicPartition("perf", 0);
			consumer.seekToBeginning(Collections.singletonList(tp));
			logger.info("partitions assigned: " + parts);
			consumer.commitSync(Collections.singletonMap(tp, new OffsetAndMetadata(0L)));
		}

		@Override
		public void onPartitionsRevokedAfterCommit(Consumer<?, ?> arg0, Collection<TopicPartition> arg1) {
		}

		@Override
		public void onPartitionsRevokedBeforeCommit(Consumer<?, ?> arg0, Collection<TopicPartition> arg1) {
		}

	}

}
