package com.aiconomy.tasks.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LedgerProperties.class)
public class LedgerClientConfig {

	@Bean
	RestClient ledgerRestClient(LedgerProperties properties) {
		return RestClient.builder()
			.baseUrl(properties.baseUrl())
			.build();
	}

}
