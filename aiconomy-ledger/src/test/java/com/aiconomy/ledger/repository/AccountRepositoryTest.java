package com.aiconomy.ledger.repository;

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

/**
 * Integration test: repository talks to a real Postgres (Testcontainers + Flyway).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class AccountRepositoryTest {

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
	private AccountRepository accountRepository;

	@Test
	void saveAndFindById() {
		UUID id = UUID.randomUUID();
		Account account = new Account(id, "agent-7", AccountType.CONSUMER, new BigDecimal("250.00"));

		accountRepository.save(account);

		Account loaded = accountRepository.findById(id).orElseThrow();
		assertThat(loaded.getOwnerId()).isEqualTo("agent-7");
		assertThat(loaded.getBalance()).isEqualByComparingTo("250.00");
		assertThat(loaded.getVersion()).isZero();
	}

	@Test
	void findByOwnerIdAndAccountType() {
		Account account = new Account(UUID.randomUUID(), "firm-1", AccountType.FIRM, BigDecimal.ZERO);
		accountRepository.save(account);

		assertThat(accountRepository.findByOwnerIdAndAccountType("firm-1", AccountType.FIRM)).isPresent();
		assertThat(accountRepository.findByOwnerIdAndAccountType("firm-1", AccountType.CONSUMER)).isEmpty();
	}

}
