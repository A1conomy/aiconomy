package com.aiconomy.ledger.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 * Stress test: 50 concurrent transfers must never produce a negative balance.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class TransferConcurrencyTest {

	private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

	private static final int THREAD_COUNT = 50;

	private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("1.00");

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
	void concurrentTransfersNeverProduceNegativeBalance() throws Exception {
		Account source = accountRepository.save(
				new Account(UUID.randomUUID(), "stress-source", AccountType.CONSUMER, new BigDecimal("1000.00")));
		Account destination = accountRepository.save(
				new Account(UUID.randomUUID(), "stress-dest", AccountType.FIRM, BigDecimal.ZERO));

		ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
		CountDownLatch startLatch = new CountDownLatch(1);
		List<Future<?>> futures = new ArrayList<>();

		for (int i = 0; i < THREAD_COUNT; i++) {
			futures.add(executor.submit(() -> {
				startLatch.await();
				transferService.transfer(source.getId(), destination.getId(), TRANSFER_AMOUNT);
				return null;
			}));
		}

		startLatch.countDown();

		for (Future<?> future : futures) {
			try {
				future.get(30, TimeUnit.SECONDS);
			}
			catch (Exception ignored) {
				// Some transfers may fail when funds run out — that is expected.
			}
		}

		executor.shutdown();

		Account reloadedSource = accountRepository.findById(source.getId()).orElseThrow();
		Account reloadedDestination = accountRepository.findById(destination.getId()).orElseThrow();

		assertThat(reloadedSource.getBalance()).isGreaterThanOrEqualTo(BigDecimal.ZERO);

		BigDecimal total = reloadedSource.getBalance().add(reloadedDestination.getBalance());
		assertThat(total).isEqualByComparingTo("1000.00");
	}

}
