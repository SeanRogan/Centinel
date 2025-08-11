package com.fedelis.centinel.monitor;

import org.springframework.boot.SpringApplication;

public class TestExchangeMonitorServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(ExchangeMonitorServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
