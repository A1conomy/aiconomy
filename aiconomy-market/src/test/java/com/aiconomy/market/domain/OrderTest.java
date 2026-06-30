package com.aiconomy.market.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.aiconomy.market.dto.SubmitOrderRequest;

class OrderTest {

	@Test
	void fromRequestCreatesOrderWithFullRemainingQuantity() {
		UUID accountId = UUID.randomUUID();

		Order order = Order.fromRequest(new SubmitOrderRequest(
				accountId,
				"WIDGET",
				OrderSide.BUY,
				new BigDecimal("10.50"),
				new BigDecimal("5.00")));

		assertThat(order.id()).isNotNull();
		assertThat(order.accountId()).isEqualTo(accountId);
		assertThat(order.symbol()).isEqualTo("WIDGET");
		assertThat(order.side()).isEqualTo(OrderSide.BUY);
		assertThat(order.price()).isEqualByComparingTo("10.50");
		assertThat(order.quantity()).isEqualByComparingTo("5.00");
		assertThat(order.remainingQuantity()).isEqualByComparingTo("5.00");
		assertThat(order.createdAt()).isNotNull();
	}

}
