package com.aiconomy.ledger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Fast smoke test — verifies Spring application context loads.
 * Does not require docker-compose; connectivity is covered by InfrastructureConnectivityTest.
 */
@SpringBootTest
@TestPropertySource(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
class LedgerApplicationTests {

	@Test
	void contextLoads() {
	}

}
