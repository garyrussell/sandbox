package com.example.sik;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StopWatch;

@SpringBootApplication
public class KperfSIApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(KperfSIApplication.class, args).close();
	}

	private final CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void run(String... arg0) throws Exception {
		latch.await();
		Thread.sleep(10_000);
	}

	@Bean
	public KafkaMessageListenerContainer<byte[], byte[]> container(ConsumerFactory<?, ?> consumerFactory) {
		ContainerProperties props = new ContainerProperties("perf");
		Map<String, Object> configs = new HashMap<>(
				((DefaultKafkaConsumerFactory<?, ?>) consumerFactory).getConfigurationProperties());
		configs.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, 2 * 1024 * 1024);
		configs.put(ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG, 1024 * 1024);
		configs.put(ConsumerConfig.CHECK_CRCS_CONFIG, false);
		props.setPollTimeout(100);
		props.setConsumerRebalanceListener(new RebalanceListener());
		return new KafkaMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(configs), props);
	}

	@Bean
	public KafkaMessageDrivenChannelAdapter<byte[], byte[]> adapter(ConsumerFactory<?, ?> consumerFactory) {
		KafkaMessageDrivenChannelAdapter<byte[], byte[]> adapter = new KafkaMessageDrivenChannelAdapter<>(
				container(consumerFactory));
		adapter.setOutputChannel(new MessageChannel() {

			private final StopWatch watch = new StopWatch("Receive 30M (SI)");

			private int n;

			@Override
			public boolean send(Message<?> m, long to) {
				if (n++ == 0) {
					this.watch.start();
				}
				else if (n == 30_000_000) {
					this.watch.stop();
					System.out.println(this.watch.toString() + "... "
							+ (30_000_000 / ((float) watch.getTotalTimeMillis() / (float) 1000))
							+ " messages per second");
					latch.countDown();
				}
				return true;
			}

			@Override
			public boolean send(Message<?> message) {
				return send(message, 0);
			}

		});
		return adapter;
	}

	public static class RebalanceListener implements ConsumerAwareRebalanceListener {

		private static final Log logger = LogFactory.getLog(RebalanceListener.class);

		@Override
		public void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> parts) {
			consumer.seekToBeginning(Collections.singletonList(new TopicPartition("perf", 0)));
			logger.info("partitions assigned: " + parts);
		}

		@Override
		public void onPartitionsRevokedAfterCommit(Consumer<?, ?> arg0, Collection<TopicPartition> arg1) {
		}

		@Override
		public void onPartitionsRevokedBeforeCommit(Consumer<?, ?> arg0, Collection<TopicPartition> arg1) {
		}

	}

}
