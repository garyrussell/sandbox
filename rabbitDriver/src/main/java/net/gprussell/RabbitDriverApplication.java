package net.gprussell;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.CacheMode;
import org.springframework.amqp.rabbit.connection.ChannelProxy;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StopWatch;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;

@SpringBootApplication
@EnableAutoConfiguration
@EnableRabbit
public class RabbitDriverApplication {

	private static CountDownLatch latch = new CountDownLatch(1);

	@Autowired
	RabbitTemplate rabbitTemplate;

	public static void main(final String[] args) throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(RabbitDriverApplication.class, args);
		latch.await();
		context.close();
	}

	@Bean
	public ConnectionFactory rabbitConnectionFactory(RabbitProperties config) {
		CachingConnectionFactory factory = new CachingConnectionFactory();
		String addresses = config.getAddresses();
		factory.setAddresses(addresses);
		if (config.getHost() != null) {
			factory.setHost(config.getHost());
			factory.setPort(config.getPort());
		}
		if (config.getUsername() != null) {
			factory.setUsername(config.getUsername());
		}
		if (config.getPassword() != null) {
			factory.setPassword(config.getPassword());
		}
		if (config.getVirtualHost() != null) {
			factory.setVirtualHost(config.getVirtualHost());
		}
		factory.setCacheMode(CacheMode.CONNECTION);
		factory.setConnectionCacheSize(50);
		return factory;
	}

	@Bean
	public SmartLifecycle runner(final Environment env) {
		return new SmartLifecycle() {

			@Override
			public int getPhase() {
				return 0;
			}

			@Override
			public void stop() {
			}

			@Override
			public void start() {
				final int count = Integer.valueOf(env.getProperty("count"));
				final String queue = env.getProperty("queue");
				int instances = Integer.valueOf(env.getProperty("instances"));
				final int messageSize = Integer.valueOf(env.getProperty("spring.message.size"));
				final boolean useTemplate = Boolean.valueOf(env.getProperty("useTemplate"));
				ExecutorService exec = Executors.newCachedThreadPool();
				StopWatch stopwatch = new StopWatch();
				stopwatch.start();
				for (int i = 0; i < instances; i++) {
					final int instance = i;
					exec.execute(new Runnable() {

						@Override
						public void run() {
							final String queueName = queue + "-" + instance;
							rabbitTemplate.execute(new ChannelCallback<Void>() {

								@Override
								public Void doInRabbit(Channel proxy) throws Exception {
									proxy.queueDeclare(queueName, false, false, true, null);
									return null;
								}
							});
							if (useTemplate) {
								System.out.println("Using RabbitTemplate");
								MessageProperties properties = MessagePropertiesBuilder.newInstance()
										.setDeliveryMode(MessageDeliveryMode.NON_PERSISTENT).build();
								for (int i = 0; i < count; i++) {
									Message message = MessageBuilder.withBody(buildMessage(i))
											.andProperties(properties).build();
									rabbitTemplate.send("", queueName, message);
								}
								rabbitTemplate.execute(new ChannelCallback<Void>() {

									@Override
									public Void doInRabbit(Channel proxy) throws Exception {
										proxy.queueDelete(queueName);
										return null;
									}
								});
							}
							else {
								System.out.println("Using native rabbit client");
								rabbitTemplate.execute(new ChannelCallback<Void>() {

									@Override
									public Void doInRabbit(Channel proxy) throws Exception {
										proxy.queueDeclare(queueName, false, false, true, null);
										Channel channel = ((ChannelProxy) proxy).getTargetChannel(); //raw channel
										BasicProperties props = new Builder().deliveryMode(1).build();
										for (int i = 0; i < count; i++) {
											byte[] body = buildMessage(i);
											channel.basicPublish("", queueName, false, props, body);
										}
										proxy.queueDelete(queueName);
										proxy.close();
										return null;
									}

								});
							}
						}

						private byte[] buildMessage(int i) {
							byte message[] = new byte[messageSize];
							try {
								ByteArrayOutputStream acc = new ByteArrayOutputStream();
								DataOutputStream d = new DataOutputStream(acc);
								long nano = System.nanoTime();
								d.writeInt(i);
								d.writeLong(nano);
								d.flush();
								acc.flush();
								byte[] body = acc.toByteArray();
								if (i % 100000 == 99999) {
									System.out.println(Thread.currentThread().getName() + " sent " + (i + 1));
								}
								if (body.length <= messageSize) {
									System.arraycopy(body, 0, message, 0, body.length);
									return message;
								}
								return body;
							}
							catch (IOException e) {
								e.printStackTrace();
								System.err.println("!!!!CRASH!!!!");
								System.exit(1);
								return null;
							}
						}

					});
				}
				exec.shutdown();
				try {
					exec.awaitTermination(10, TimeUnit.HOURS);
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				stopwatch.stop();
				System.out.println("Elapsed seconds: " + stopwatch.getTotalTimeSeconds());
				latch.countDown();
			}

			@Override
			public boolean isRunning() {
				return false;
			}

			@Override
			public void stop(Runnable arg0) {
			}

			@Override
			public boolean isAutoStartup() {
				return true;
			}

		};
	}

}
