package com.example;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StopWatch;

@SpringBootApplication
public class Shard1Application implements CommandLineRunner {

	private static final int COUNT = 300_000;

	private static final String SHARDED_EXCHANGE = "shard";

	public static void main(String[] args) {
		SpringApplication.run(Shard1Application.class, args).close();
	}

	@Autowired
	private RabbitTemplate template;

	private final CountDownLatch latch = new CountDownLatch(COUNT);

	@Override
	public void run(String... args) throws Exception {
		StopWatch watch = new StopWatch();
		watch.start();
		// comment this out if the shard is already populated
//		for (int i = 0; i < COUNT; i++) {
//			this.template.convertAndSend(SHARDED_EXCHANGE, "foo", "bar");
//		}
		System.out.println("Sent");
		this.latch.await(100, TimeUnit.SECONDS);
		watch.stop();
		System.out.println("Runtime: " + watch.getTotalTimeMillis() + ", msgs per second: "
				+ (COUNT / watch.getTotalTimeSeconds()));
	}

	@RabbitListener(queues = SHARDED_EXCHANGE, concurrency = "6")
	public void listen(Object in) {
		latch.countDown();
	}

	@Bean
	public Exchange shard() {
		return new CustomExchange(SHARDED_EXCHANGE, "x-random");
	}

	/**
	 * Needed to distribute shards across consumers
	 * @param cf the cf.
	 * @return a dummy bean.
	 */
	@Bean
	public Object slowDownConsumers(CachingConnectionFactory cf) {
		final Semaphore semaphore = new Semaphore(1);
		cf.addChannelListener((c, t) -> {
			try {
				semaphore.acquire(1);
				Thread.sleep(500);
				semaphore.release(1);
				System.out.println(Thread.currentThread().getName() + " next channel " + c);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		});
		return new Object();
	}

}
