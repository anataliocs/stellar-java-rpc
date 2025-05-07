package org.stellar.stellarjavarpc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SuppressWarnings("ClassWithoutLogger")
@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "rpcTaskExecutor")
	public TaskExecutor rpcTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(25);
		executor.setThreadNamePrefix("rpc-pool-");
		executor.initialize();
		return executor;
	}
}