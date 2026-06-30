package com.aiconomy.market.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection settings for the Core Banking Ledger service.
 */
@ConfigurationProperties(prefix = "aiconomy.ledger")
public record LedgerProperties(

		String baseUrl

) {
}
