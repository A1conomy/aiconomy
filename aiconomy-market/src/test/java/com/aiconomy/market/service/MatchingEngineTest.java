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

import com.aiconomy.market.domain.OrderSide;
import com.aiconomy.market.dto.SubmitOrderRequest;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MatchingEngineTest {

	private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

	private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("apache/kafka:3.8.1");

	@Container
	static KafkaContainer kafka = new KafkaContainer(KAFKA_IMAGE);

	@Container
	@SuppressWarnings("resource")
	static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

	@Autowired
	private MatchingEngine matchingEngine;

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
	void incomingBuyMatchesRestingAskAtAskPrice() {
		UUID seller = UUID.randomUUID();
		UUID buyer = UUID.randomUUID();

		orderBookStore.addOrder(com.aiconomy.market.domain.Order.fromRequest(
				new SubmitOrderRequest(seller, "WIDGET", OrderSide.SELL, new BigDecimal("10.00"), new BigDecimal("5.00"))));

		MatchResult result = matchingEngine.submitOrder(
				new SubmitOrderRequest(buyer, "WIDGET", OrderSide.BUY, new BigDecimal("12.00"), new BigDecimal("3.00")));

		assertThat(result.trades()).hasSize(1);
		assertThat(result.trades().getFirst().price()).isEqualByComparingTo("10.00");
		assertThat(result.trades().getFirst().quantity()).isEqualByComparingTo("3.00");
		assertThat(result.trades().getFirst().buyerAccountId()).isEqualTo(buyer);
		assertThat(result.trades().getFirst().sellerAccountId()).isEqualTo(seller);
		assertThat(result.restingOrder()).isEmpty();
		assertThat(orderBookStore.getBestAskPrice("WIDGET")).contains(new BigDecimal("10.00"));
		assertThat(orderBookStore.peekBestOrder("WIDGET", OrderSide.SELL)).get()
			.extracting(order -> order.remainingQuantity())
			.isEqualTo(new BigDecimal("2.00"));
	}

	@Test
	void nonCrossingOrdersRestOnBook() {
		UUID bidAccount = UUID.randomUUID();
		UUID askAccount = UUID.randomUUID();

		matchingEngine.submitOrder(
				new SubmitOrderRequest(bidAccount, "WIDGET", OrderSide.BUY, new BigDecimal("9.00"), new BigDecimal("4.00")));
		MatchResult result = matchingEngine.submitOrder(
				new SubmitOrderRequest(askAccount, "WIDGET", OrderSide.SELL, new BigDecimal("10.00"), new BigDecimal("2.00")));

		assertThat(result.trades()).isEmpty();
		assertThat(result.restingOrder()).isPresent();
		assertThat(orderBookStore.getBestBidPrice("WIDGET")).contains(new BigDecimal("9.00"));
		assertThat(orderBookStore.getBestAskPrice("WIDGET")).contains(new BigDecimal("10.00"));
	}

	@Test
	void partialFillThenRestIncomingRemainder() {
		UUID seller = UUID.randomUUID();
		UUID buyer = UUID.randomUUID();

		orderBookStore.addOrder(com.aiconomy.market.domain.Order.fromRequest(
				new SubmitOrderRequest(seller, "WIDGET", OrderSide.SELL, new BigDecimal("10.00"), new BigDecimal("2.00"))));

		MatchResult result = matchingEngine.submitOrder(
				new SubmitOrderRequest(buyer, "WIDGET", OrderSide.BUY, new BigDecimal("10.00"), new BigDecimal("5.00")));

		assertThat(result.trades()).hasSize(1);
		assertThat(result.trades().getFirst().quantity()).isEqualByComparingTo("2.00");
		assertThat(result.restingOrder()).isPresent();
		assertThat(result.restingOrder().get().remainingQuantity()).isEqualByComparingTo("3.00");
		assertThat(result.restingOrder().get().side()).isEqualTo(OrderSide.BUY);
		assertThat(orderBookStore.getBestAskPrice("WIDGET")).isEmpty();
		assertThat(orderBookStore.getBestBidPrice("WIDGET")).contains(new BigDecimal("10.00"));
	}

}
