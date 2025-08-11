package com.fedelis.centinel.analysis;

import org.springframework.boot.SpringApplication;

public class TestAnalysisApplication {

	public static void main(String[] args) {
		SpringApplication.from(AnalysisApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
