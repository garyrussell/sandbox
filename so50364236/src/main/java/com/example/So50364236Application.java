package com.example;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.qpid.server.Broker;
import org.apache.qpid.server.BrokerOptions;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@SpringBootApplication
public class So50364236Application {

	public static void main(String[] args) {
		new SpringApplicationBuilder(So50364236Application.class)
			.web(WebApplicationType.NONE)
			.run(args);
	}

	@Bean
	public ApplicationRunner runner(RabbitTemplate template) {
		return args -> template.convertAndSend("so50364236b", "foo");
	}

	@Bean
	BrokerOptions brokerOptions() throws Exception {

		Path tmpFolder = Files.createTempDirectory("qpidWork");
		Path homeFolder = Files.createTempDirectory("qpidHome");
		File etc = new File(homeFolder.toFile(), "etc");
		etc.mkdir();
		FileOutputStream fos = new FileOutputStream(new File(etc, "passwd"));
		fos.write("guest:guest\n".getBytes());
		fos.close();

		BrokerOptions brokerOptions = new BrokerOptions();

		brokerOptions.setConfigProperty("qpid.work_dir", tmpFolder.toAbsolutePath().toString());
		brokerOptions.setConfigProperty("qpid.amqp_port", "8888");
		brokerOptions.setConfigProperty("qpid.home_dir", homeFolder.toAbsolutePath().toString());
		Resource config = new ClassPathResource("qpid-config2.json");
		brokerOptions.setInitialConfigurationLocation(config.getFile().getAbsolutePath());

		return brokerOptions;
	}

	@Bean
	Broker broker() throws Exception {
		Broker broker = new Broker();
		broker.startup(brokerOptions());
		return broker;
	}

	@RabbitListener(queues = "so50364236b")
	public void listen(String in) {
		System.out.println("received: " + in);
	}

	@Bean
	public Queue queue() {
		return new Queue("so50364236b");
	}

}

//@Bean
//public SmartLifecycle broker() {
//	return new SmartLifecycle() {
//
//		final SystemLauncher systemLauncher = new SystemLauncher();
//
//		private boolean running;
//
//		@Override
//		public int getPhase() {
//			return Integer.MIN_VALUE;
//		}
//
//		@Override
//		public void stop() {
//			if (this.running) {
//				this.systemLauncher.shutdown();
//			}
//			this.running = false;
//		}
//
//		@Override
//		public void start() {
//			if (!this.running) {
//				try {
//					Map<String, Object> config = new HashMap<>();
//					Resource configjson = new ClassPathResource("qpid-config.json");
//					config.put(SystemConfig.TYPE, "JSON");
//					config.put(SystemConfig.INITIAL_CONFIGURATION_LOCATION,
//							configjson.getFile().getAbsolutePath());
//					config.put(SystemConfig.STARTUP_LOGGED_TO_SYSTEM_OUT, true);
//
//					Path tmpFolder = Files.createTempDirectory("qpidWork");
//					Path homeFolder = Files.createTempDirectory("qpidHome");
//					File etc = new File(homeFolder.toFile(), "etc");
//					etc.mkdir();
//					FileOutputStream fos = new FileOutputStream(new File(etc, "passwd"));
//					fos.write("guest:guest\n".getBytes());
//					fos.close();
//					File props = new File(etc, "config.properties");
//					fos = new FileOutputStream(props);
//					fos.write("qpid.amqp_port=8888".getBytes());
//					fos.write(("\nqpid.home_dir=" + homeFolder.toAbsolutePath().toString()).getBytes());
//					fos.write("\n".getBytes());
//					fos.close();
//
//					config.put(SystemConfig.QPID_WORK_DIR, tmpFolder.toAbsolutePath().toString());
//					config.put(SystemConfig.INITIAL_SYSTEM_PROPERTIES_LOCATION, props.getAbsolutePath());
//
//					this.systemLauncher.startup(config);
//					this.running = true;
//				}
//				catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}
//
//		@Override
//		public boolean isRunning() {
//			return this.running;
//		}
//
//		@Override
//		public void stop(Runnable callback) {
//			stop();
//			callback.run();
//		}
//
//		@Override
//		public boolean isAutoStartup() {
//			return true;
//		}
//
//	};
//}

