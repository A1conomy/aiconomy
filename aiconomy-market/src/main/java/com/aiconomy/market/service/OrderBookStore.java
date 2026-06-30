package com.aiconomy.market.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.aiconomy.market.domain.Order;
import com.aiconomy.market.domain.OrderSide;

/**
 * Redis-backed order book projection. Hot matching state lives here; Kafka carries the audit trail.
 *
 * <p>Keys:
 * <ul>
 *   <li>{@code market:order:{id}} — order hash (source of truth for resting order fields)</li>
 *   <li>{@code market:book:{symbol}:bids} — ZSET, score = limit price</li>
 *   <li>{@code market:book:{symbol}:asks} — ZSET, score = limit price</li>
 * </ul>
 * ZSET member encodes price-time priority: {@code {epochMillis}:{orderId}}.
 */
@Service
public class OrderBookStore {

	private static final String ORDER_KEY_PREFIX = "market:order:";

	private static final String BOOK_KEY_PREFIX = "market:book:";

	private final StringRedisTemplate redisTemplate;

	public OrderBookStore(StringRedisTemplate redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	public void addOrder(Order order) {
		String orderKey = orderKey(order.id());
		redisTemplate.opsForHash().putAll(orderKey, toHash(order));
		redisTemplate.opsForZSet().add(bookKey(order.symbol(), order.side()), zsetMember(order), order.price().doubleValue());
	}

	public Optional<Order> findById(UUID orderId) {
		Map<Object, Object> entries = redisTemplate.opsForHash().entries(orderKey(orderId));
		if (entries.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(fromHash(orderId, entries));
	}

	public Optional<BigDecimal> getBestBidPrice(String symbol) {
		return bestPrice(symbol, OrderSide.BUY);
	}

	public Optional<BigDecimal> getBestAskPrice(String symbol) {
		return bestPrice(symbol, OrderSide.SELL);
	}

	public Optional<Order> peekBestOrder(String symbol, OrderSide side) {
		var zset = redisTemplate.opsForZSet();
		String key = bookKey(symbol, side);
		var top = side == OrderSide.BUY ? zset.reverseRange(key, 0, 0) : zset.range(key, 0, 0);
		if (top == null || top.isEmpty()) {
			return Optional.empty();
		}
		return findById(parseOrderId(top.iterator().next()));
	}

	public void updateRemainingQuantity(UUID orderId, BigDecimal remainingQuantity) {
		redisTemplate.opsForHash().put(orderKey(orderId), "remainingQuantity", remainingQuantity.toPlainString());
	}

	public void removeOrder(Order order) {
		redisTemplate.delete(orderKey(order.id()));
		redisTemplate.opsForZSet().remove(bookKey(order.symbol(), order.side()), zsetMember(order));
	}

	private Optional<BigDecimal> bestPrice(String symbol, OrderSide side) {
		var zset = redisTemplate.opsForZSet();
		String bookKey = bookKey(symbol, side);
		var top = side == OrderSide.BUY ? zset.reverseRangeWithScores(bookKey, 0, 0) : zset.rangeWithScores(bookKey, 0, 0);
		if (top == null || top.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(BigDecimal.valueOf(top.iterator().next().getScore()));
	}

	private static String orderKey(UUID orderId) {
		return ORDER_KEY_PREFIX + orderId;
	}

	private static String bookKey(String symbol, OrderSide side) {
		String leg = side == OrderSide.BUY ? "bids" : "asks";
		return BOOK_KEY_PREFIX + symbol + ":" + leg;
	}

	private static String zsetMember(Order order) {
		return order.createdAt().toEpochMilli() + ":" + order.id();
	}

	private static UUID parseOrderId(String member) {
		int separator = member.indexOf(':');
		return UUID.fromString(member.substring(separator + 1));
	}

	private static Map<String, String> toHash(Order order) {
		return Map.of(
				"accountId", order.accountId().toString(),
				"symbol", order.symbol(),
				"side", order.side().name(),
				"price", order.price().toPlainString(),
				"quantity", order.quantity().toPlainString(),
				"remainingQuantity", order.remainingQuantity().toPlainString(),
				"createdAt", order.createdAt().toString());
	}

	private static Order fromHash(UUID orderId, Map<Object, Object> entries) {
		return new Order(
				orderId,
				UUID.fromString(stringField(entries, "accountId")),
				stringField(entries, "symbol"),
				OrderSide.valueOf(stringField(entries, "side")),
				new BigDecimal(stringField(entries, "price")),
				new BigDecimal(stringField(entries, "quantity")),
				new BigDecimal(stringField(entries, "remainingQuantity")),
				Instant.parse(stringField(entries, "createdAt")));
	}

	private static String stringField(Map<Object, Object> entries, String field) {
		Object value = entries.get(field);
		if (value == null) {
			throw new IllegalStateException("Missing Redis hash field: " + field);
		}
		return value.toString();
	}

}
