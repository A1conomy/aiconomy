package com.aiconomy.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.aiconomy.ledger.domain.Account;
import com.aiconomy.ledger.domain.AccountType;
import com.aiconomy.ledger.repository.AccountRepository;

/**
 * End-to-end transfer against real Postgres + Flyway.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TransferServiceIntegrationTest {

	private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private TransferService transferService;

	@Autowired
	private AccountRepository accountRepository;

	@Test
	void transferPersistsNewBalancesInDatabase() {
		Account source = accountRepository.save(
				new Account(UUID.randomUUID(), "c-1", AccountType.CONSUMER, new BigDecimal("200.00")));
		Account destination = accountRepository.save(
				new Account(UUID.randomUUID(), "f-1", AccountType.FIRM, new BigDecimal("0.00")));

		transferService.transfer(source.getId(), destination.getId(), new BigDecimal("75.50"));

		Account reloadedSource = accountRepository.findById(source.getId()).orElseThrow();
		Account reloadedDestination = accountRepository.findById(destination.getId()).orElseThrow();

		assertThat(reloadedSource.getBalance()).isEqualByComparingTo("124.50");
		assertThat(reloadedDestination.getBalance()).isEqualByComparingTo("75.50");
		assertThat(reloadedSource.getVersion()).isEqualTo(1L);
	}

}
