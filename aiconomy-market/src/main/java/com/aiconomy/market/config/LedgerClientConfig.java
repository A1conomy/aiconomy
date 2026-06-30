package com.aiconomy.market.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(LedgerProperties.class)
public class LedgerClientConfig {

	@Bean
	RestClient ledgerRestClient(LedgerProperties ledgerProperties) {
		return RestClient.builder()
			.baseUrl(ledgerProperties.baseUrl())
			.build();
	}

}
