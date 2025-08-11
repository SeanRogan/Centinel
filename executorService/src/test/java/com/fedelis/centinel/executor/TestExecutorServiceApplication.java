package com.fedelis.centinel.executor;

import org.springframework.boot.SpringApplication;

public class TestExecutorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(ExecutorServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
