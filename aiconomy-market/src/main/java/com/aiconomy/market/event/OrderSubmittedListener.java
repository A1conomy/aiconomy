package com.aiconomy.market.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.aiconomy.common.event.OrderSubmittedEvent;
import com.aiconomy.common.kafka.KafkaTopics;
import com.aiconomy.market.domain.OrderSide;
import com.aiconomy.market.dto.SubmitOrderRequest;
import com.aiconomy.market.service.OrderService;
import tools.jackson.databind.ObjectMapper;

/**
 * Consumes {@link KafkaTopics#ORDERS_SUBMITTED} and routes orders through the matching engine.
 */
@Component
public class OrderSubmittedListener {

	private static final Logger log = LoggerFactory.getLogger(OrderSubmittedListener.class);

	private final OrderService orderService;

	private final ObjectMapper objectMapper;

	public OrderSubmittedListener(OrderService orderService, ObjectMapper objectMapper) {
		this.orderService = orderService;
		this.objectMapper = objectMapper;
	}

	@KafkaListener(topics = KafkaTopics.ORDERS_SUBMITTED, groupId = "aiconomy-market-orders")
	public void onOrderSubmitted(String payload) {
		try {
			OrderSubmittedEvent event = objectMapper.readValue(payload, OrderSubmittedEvent.class);
			log.info("Kafka order received: eventId={} side={} symbol={} price={} qty={}",
					event.eventId(), event.side(), event.symbol(), event.price(), event.quantity());

			SubmitOrderRequest request = new SubmitOrderRequest(
					event.accountId(),
					event.symbol(),
					OrderSide.valueOf(event.side()),
					event.price(),
					event.quantity());

			orderService.submitOrder(request);
		}
		catch (Exception ex) {
			log.error("Failed to process order event: {}", payload, ex);
			throw new IllegalStateException("Invalid order event payload", ex);
		}
	}

}
