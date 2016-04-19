package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.ip.tcp.connection.TcpServerConnectionFactory;
import org.springframework.util.StopWatch;

@SpringBootApplication
@ImportResource("context.xml")
public class So36667075Application {

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(So36667075Application.class)
				.web(false)
				.run(args);
		int port = context.getBean(TcpServerConnectionFactory.class).getPort();
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		final BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		int count = 1000;
		final CountDownLatch latch = new CountDownLatch(count);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(new Runnable() {

			@Override
			public void run() {
				try {
					while (reader.readLine() != null) {
						latch.countDown();
					}
				}
				catch (IOException e) {
				}
			}
		});
		OutputStream os = socket.getOutputStream();
		StopWatch watch = new StopWatch();
		byte[] bytes = "foo\r\n".getBytes();
		watch.start();
		for (int i = 0; i < count; i++) {
			os.write(bytes);
		}
		if (!latch.await(10, TimeUnit.SECONDS)) {
			System.err.println("Failed to receive replies in 10 seconds");
		}
		else {
			watch.stop();
			System.out.println("Sent and received " + count + " messages in " + watch.getTotalTimeMillis() + " ms.");
		}
		socket.close();
		exec.shutdownNow();
		context.close();
	}

}
