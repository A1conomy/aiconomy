package com.aiconomy.ledger;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies Spring Boot can connect to Postgres, Kafka, and Redis via Testcontainers.
 * Skipped automatically when Docker is not available.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class InfrastructureConnectivityTest {

	private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

	private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.1");

	private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

	@Container
	static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

	@Container
	@SuppressWarnings("resource")
	static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Test
	void postgresAcceptsConnection() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection.isValid(2)).isTrue();
		}
	}

	@Test
	void kafkaCanPublishAndConsume() {
		String topic = "infra.connectivity.test";
		String message = "connectivity-test";

		kafkaTemplate.send(topic, message);

		try (Consumer<String, String> consumer = createConsumer(topic)) {
			ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofSeconds(10));
			assertThat(records.count()).isGreaterThanOrEqualTo(1);
			assertThat(records.iterator().next().value()).isEqualTo(message);
		}
	}

	@Test
	void redisRespondsToPing() {
		String key = "infra:connectivity:test";
		redisTemplate.opsForValue().set(key, "ok");
		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("ok");
	}

	private Consumer<String, String> createConsumer(String topic) {
		Map<String, Object> consumerProps = new HashMap<>();
		consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
		consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
				consumerProps,
				new StringDeserializer(),
				new StringDeserializer()).createConsumer();
		consumer.subscribe(List.of(topic));
		return consumer;
	}

}
