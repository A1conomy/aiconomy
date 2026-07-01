package com.aiconomy.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aiconomy.market.domain.Trade;
import com.aiconomy.market.dto.SubmitOrderRequest;
import com.aiconomy.market.dto.SubmitOrderResponse;
import com.aiconomy.market.domain.OrderSide;
import com.aiconomy.market.event.TradeEventPublisher;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

	@Mock
	private MatchingEngine matchingEngine;

	@Mock
	private LedgerSettlementClient ledgerSettlementClient;

	@Mock
	private TradeEventPublisher tradeEventPublisher;

	@InjectMocks
	private OrderService orderService;

	@Test
	void settlesEveryTradeProducedByMatchingEngine() {
		SubmitOrderRequest request = new SubmitOrderRequest(
				UUID.randomUUID(), "WIDGET", OrderSide.BUY, new BigDecimal("10.00"), new BigDecimal("2.00"));
		Trade trade = sampleTrade();
		when(matchingEngine.submitOrder(request)).thenReturn(new MatchResult(List.of(trade), Optional.empty()));

		SubmitOrderResponse response = orderService.submitOrder(request);

		verify(ledgerSettlementClient).settle(trade);
		verify(tradeEventPublisher).publish(trade);
		assertThat(response.trades()).hasSize(1);
		assertThat(response.trades().getFirst().settlementAmount()).isEqualByComparingTo("20.00");
		assertThat(response.restingOrder()).isNull();
	}

	@Test
	void skipsSettlementWhenOrderRestsWithoutTrades() {
		SubmitOrderRequest request = new SubmitOrderRequest(
				UUID.randomUUID(), "WIDGET", OrderSide.SELL, new BigDecimal("15.00"), new BigDecimal("1.00"));
		when(matchingEngine.submitOrder(request)).thenReturn(new MatchResult(List.of(), Optional.empty()));

		SubmitOrderResponse response = orderService.submitOrder(request);

		verifyNoInteractions(ledgerSettlementClient);
		assertThat(response.trades()).isEmpty();
	}

	private static Trade sampleTrade() {
		UUID buyOrderId = UUID.randomUUID();
		UUID sellOrderId = UUID.randomUUID();
		UUID buyer = UUID.randomUUID();
		UUID seller = UUID.randomUUID();
		return new Trade(
				UUID.randomUUID(),
				"WIDGET",
				new BigDecimal("10.00"),
				new BigDecimal("2.00"),
				buyOrderId,
				sellOrderId,
				buyer,
				seller,
				Instant.parse("2026-06-30T12:00:00Z"));
	}

}
