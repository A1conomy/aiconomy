package com.aiconomy.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Core Banking Ledger service.
 * M0c: infrastructure connectivity skeleton. Business logic arrives in M1.
 */
@SpringBootApplication
public class LedgerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LedgerApplication.class, args);
	}

}
