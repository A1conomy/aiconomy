package com.aiconomy.market.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.aiconomy.common.event.OrderSubmittedEvent;
import com.aiconomy.common.event.TradeExecutedEvent;
import com.aiconomy.common.kafka.KafkaTopics;
import tools.jackson.databind.ObjectMapper;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Verifies the Kafka pipeline: orders.submitted → match/settle → trades.executed.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class KafkaOrderPipelineTest {

	private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.1");

	private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

	private static final MockWebServer LEDGER_SERVER = new MockWebServer();

	static {
		try {
			LEDGER_SERVER.start();
		}
		catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	@Container
	static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

	@Container
	@SuppressWarnings("resource")
	static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Autowired
	private ObjectMapper objectMapper;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
		registry.add("aiconomy.ledger.base-url", () -> LEDGER_SERVER.url("/").toString());
	}

	@AfterAll
	static void shutdownLedgerServer() throws IOException {
		LEDGER_SERVER.shutdown();
	}

	@BeforeEach
	void clearRedis() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@Test
	void orderSubmittedEventsProduceTradeExecutedEvent() throws Exception {
		UUID seller = UUID.randomUUID();
		UUID buyer = UUID.randomUUID();

		publishOrder(new OrderSubmittedEvent(
				UUID.randomUUID(), seller, "WIDGET", "SELL", new BigDecimal("10.00"), new BigDecimal("5.00"), Instant.now()));

		LEDGER_SERVER.enqueue(new MockResponse().setResponseCode(204));

		publishOrder(new OrderSubmittedEvent(
				UUID.randomUUID(), buyer, "WIDGET", "BUY", new BigDecimal("12.00"), new BigDecimal("3.00"), Instant.now()));

		try (Consumer<String, String> consumer = createTradeConsumer()) {
			ConsumerRecords<String, String> records = consumer.poll(java.time.Duration.ofSeconds(15));
			assertThat(records.count()).isGreaterThanOrEqualTo(1);

			TradeExecutedEvent event = objectMapper.readValue(records.iterator().next().value(), TradeExecutedEvent.class);
			assertThat(event.symbol()).isEqualTo("WIDGET");
			assertThat(event.price()).isEqualByComparingTo("10.00");
			assertThat(event.quantity()).isEqualByComparingTo("3.00");
			assertThat(event.settlementAmount()).isEqualByComparingTo("30.00");
			assertThat(event.buyerAccountId()).isEqualTo(buyer);
			assertThat(event.sellerAccountId()).isEqualTo(seller);
		}
	}

	private void publishOrder(OrderSubmittedEvent event) throws Exception {
		Map<String, Object> props = new HashMap<>();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

		String payload = objectMapper.writeValueAsString(event);

		try (KafkaProducer<String, String> producer = new KafkaProducer<>(props, new StringSerializer(), new StringSerializer())) {
			producer.send(new ProducerRecord<>(KafkaTopics.ORDERS_SUBMITTED, event.eventId().toString(), payload));
			producer.flush();
		}

		Thread.sleep(500);
	}

	private Consumer<String, String> createTradeConsumer() {
		Map<String, Object> props = new HashMap<>();
		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		props.put(ConsumerConfig.GROUP_ID_CONFIG, "kafka-pipeline-test-" + UUID.randomUUID());
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

		Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
				props,
				new StringDeserializer(),
				new StringDeserializer()).createConsumer();
		consumer.subscribe(List.of(KafkaTopics.TRADES_EXECUTED));
		return consumer;
	}

}
