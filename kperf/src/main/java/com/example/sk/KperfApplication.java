package com.example.sk;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.util.StopWatch;

//@SpringBootApplication
public class KperfApplication implements CommandLineRunner {

//	public static void main(String[] args) {
//		SpringApplication.run(KperfApplication.class, args).close();
//	}

//	@Autowired
//	private KafkaTemplate<byte[], byte[]> template;

	private final CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void run(String... arg0) throws Exception {
//		byte[] bytes = new byte[1024];
//		for (int i = 0; i < 30_000_000; i++) {
//			template.send("perf", bytes);
//		}
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
		props.setMessageListener(new Listener(this.latch));
		return new KafkaMessageListenerContainer<>(new DefaultKafkaConsumerFactory<>(configs), props);
	}

	public static class Listener implements MessageListener<byte[], byte[]>, ConsumerSeekAware {

		private final CountDownLatch latch;

		private final StopWatch watch = new StopWatch("Receive 30M");

		private int n;

		public Listener(CountDownLatch latch) {
			this.latch = latch;
		}


		@Override
		public void onMessage(ConsumerRecord<byte[], byte[]> record) {
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
		}


		@Override
		public void registerSeekCallback(ConsumerSeekCallback callback) {
		}

		@Override
		public void onPartitionsAssigned(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
			callback.seekToBeginning("perf", 0);
		}

		@Override
		public void onIdleContainer(Map<TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
		}

	}

}
