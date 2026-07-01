package com.aiconomy.market.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.aiconomy.market.domain.Order;
import com.aiconomy.market.domain.OrderSide;
import com.aiconomy.market.dto.SubmitOrderRequest;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OrderBookStoreTest {

	private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

	private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.1");

	@Container
	static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

	@Container
	@SuppressWarnings("resource")
	static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

	@Autowired
	private OrderBookStore orderBookStore;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@BeforeEach
	void clearRedis() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@Test
	void addOrderStoresHashAndUpdatesBestPrices() {
		Order bid = Order.fromRequest(new SubmitOrderRequest(
				UUID.randomUUID(), "WIDGET", OrderSide.BUY, new BigDecimal("9.00"), new BigDecimal("1.00")));
		Order ask = Order.fromRequest(new SubmitOrderRequest(
				UUID.randomUUID(), "WIDGET", OrderSide.SELL, new BigDecimal("10.00"), new BigDecimal("2.00")));

		orderBookStore.addOrder(bid);
		orderBookStore.addOrder(ask);

		assertThat(orderBookStore.findById(bid.id())).get()
			.satisfies(stored -> {
				assertThat(stored.accountId()).isEqualTo(bid.accountId());
				assertThat(stored.remainingQuantity()).isEqualByComparingTo("1.00");
			});
		assertThat(orderBookStore.getBestBidPrice("WIDGET")).hasValueSatisfying(
				price -> assertThat(price).isEqualByComparingTo("9.00"));
		assertThat(orderBookStore.getBestAskPrice("WIDGET")).hasValueSatisfying(
				price -> assertThat(price).isEqualByComparingTo("10.00"));
	}

	@Test
	void updateRemainingQuantityAndRemoveOrder() {
		Order order = Order.fromRequest(new SubmitOrderRequest(
				UUID.randomUUID(), "WIDGET", OrderSide.SELL, new BigDecimal("12.00"), new BigDecimal("4.00")));

		orderBookStore.addOrder(order);
		orderBookStore.updateRemainingQuantity(order.id(), new BigDecimal("1.50"));

		assertThat(orderBookStore.findById(order.id())).get()
			.extracting(Order::remainingQuantity)
			.isEqualTo(new BigDecimal("1.50"));

		orderBookStore.removeOrder(order);

		assertThat(orderBookStore.findById(order.id())).isEmpty();
		assertThat(orderBookStore.getBestAskPrice("WIDGET")).isEmpty();
	}

}
