package com.example.scst;

import java.util.concurrent.CountDownLatch;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StopWatch;

@SpringBootApplication
@EnableBinding(Sink.class)
public class KperfScstApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(KperfScstApplication.class, args).close();
	}

	private final CountDownLatch latch = new CountDownLatch(1);

	@Override
	public void run(String... arg0) throws Exception {
		latch.await();
		Thread.sleep(10_000);
	}

	@Bean
	public Listener foo() {
		return new Listener(latch);
	}

	public static class Listener {

		private final CountDownLatch latch;

		private final StopWatch watch = new StopWatch("Receive 30M Stream");

		private int n;

		public Listener(CountDownLatch latch) {
			this.latch = latch;
		}

		@StreamListener(Sink.INPUT)
		public void listen(byte[] bytes) {
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

	}
}
