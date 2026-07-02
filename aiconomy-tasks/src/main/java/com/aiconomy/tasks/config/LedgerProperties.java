package com.aiconomy.tasks.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ledger service connection settings for escrow calls.
 */
@ConfigurationProperties(prefix = "ledger")
public record LedgerProperties(String baseUrl) {
}
