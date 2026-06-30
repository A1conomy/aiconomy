package com.aiconomy.market;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Verifies the market service can connect to Redis (order book) and Kafka (events).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class InfrastructureConnectivityTest {

	private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.1");

	private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

	@Container
	static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

	@Container
	@SuppressWarnings("resource")
	static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Test
	void kafkaCanPublishAndConsume() {
		String topic = "infra.connectivity.test";
		String message = "market-connectivity-test";

		kafkaTemplate.send(topic, message);

		try (Consumer<String, String> consumer = createConsumer(topic)) {
			ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofSeconds(10));
			assertThat(records.count()).isGreaterThanOrEqualTo(1);
			assertThat(records.iterator().next().value()).isEqualTo(message);
		}
	}

	@Test
	void redisRespondsToPing() {
		String key = "market:infra:connectivity";
		redisTemplate.opsForValue().set(key, "ok");
		assertThat(redisTemplate.opsForValue().get(key)).isEqualTo("ok");
	}

	private Consumer<String, String> createConsumer(String topic) {
		Map<String, Object> consumerProps = new HashMap<>();
		consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "market-test-group");
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
