package com.aiconomy.market.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.aiconomy.common.event.TradeExecutedEvent;
import com.aiconomy.common.kafka.KafkaTopics;
import com.aiconomy.market.domain.Trade;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Publishes {@link KafkaTopics#TRADES_EXECUTED} after successful ledger settlement.
 */
@Component
public class TradeEventPublisher {

	private static final Logger log = LoggerFactory.getLogger(TradeEventPublisher.class);

	private final KafkaTemplate<String, String> kafkaTemplate;

	private final ObjectMapper objectMapper;

	public TradeEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
		this.kafkaTemplate = kafkaTemplate;
		this.objectMapper = objectMapper;
	}

	public void publish(Trade trade) {
		TradeExecutedEvent event = new TradeExecutedEvent(
				trade.id(),
				trade.symbol(),
				trade.price(),
				trade.quantity(),
				trade.buyOrderId(),
				trade.sellOrderId(),
				trade.buyerAccountId(),
				trade.sellerAccountId(),
				trade.settlementAmount(),
				trade.executedAt());

		try {
			String payload = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(KafkaTopics.TRADES_EXECUTED, trade.id().toString(), payload);
			log.info("Trade event published: tradeId={} symbol={} settlement={}",
					trade.id(), trade.symbol(), trade.settlementAmount());
		}
		catch (JacksonException ex) {
			throw new IllegalStateException("Failed to serialize trade event", ex);
		}
	}

}
