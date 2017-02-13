package com.example;

import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.ftp.session.DefaultFtpSessionFactory;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;
import org.springframework.messaging.Message;

@SpringBootApplication
@EnableBinding(Sink.class)
public class FtpstreamoutApplication {

	private static final String QUEUE = "foo.group";

	private final Map<String, OutputStream> streams = new ConcurrentHashMap<>();

	private final CountDownLatch latch = new CountDownLatch(1);

	@Autowired
	private RabbitTemplate template;

	public static void main(String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(FtpstreamoutApplication.class, args);
		context.getBean(FtpstreamoutApplication.class).sendChunks();
		context.close();
	}

	public void sendChunks() throws Exception {
		this.template.convertAndSend(QUEUE, "foo", m -> {
			m.getMessageProperties().setHeader("control", "first");
			m.getMessageProperties().setHeader("file_name", "fooStream.txt");
			return m;
		});
		this.template.convertAndSend(QUEUE, "bar", m -> {
			m.getMessageProperties().setHeader("file_name", "fooStream.txt");
			return m;
		});
		this.template.convertAndSend(QUEUE, "baz", m -> {
			m.getMessageProperties().setHeader("control", "last");
			m.getMessageProperties().setHeader("file_name", "fooStream.txt");
			return m;
		});
		this.latch.await(10, TimeUnit.SECONDS);
	}

	@Bean
	public DefaultFtpSessionFactory sf() {
		DefaultFtpSessionFactory sf = new DefaultFtpSessionFactory();
		sf.setHost("10.0.0.3");
		sf.setUsername("ftptest");
		sf.setPassword("ftptest");
		return sf;
	}

	@Bean
	public FtpRemoteFileTemplate remoteFileTemplate() {
		return new FtpRemoteFileTemplate(sf());
	}

	@StreamListener(Sink.INPUT)
	public void handle(Message<String> message) throws Exception {
		String file = (String) message.getHeaders().get("file_name");
		Object controlHeader = message.getHeaders().get("control");
		if ("first".equals(controlHeader)) {
			PipedOutputStream pos = new PipedOutputStream();
			PipedInputStream pis = new PipedInputStream(pos);
			this.streams.put(file, pos);
			Executors.newSingleThreadExecutor().execute(() -> {
					Boolean result = remoteFileTemplate().execute(s -> {
						s.write(pis, file);
						return true;
					});
					System.out.println("Sent " + file + ": " + result);
				});
		}
		this.streams.get(file).write(message.getPayload().getBytes());
		this.streams.get(file).write(0x0a);
		if ("last".equals(controlHeader)) {
			this.streams.remove(file).close();
			this.latch.countDown();
		}
	}

}
