package com.aiconomy.market.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.aiconomy.market.domain.OrderSide;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for submitting a limit order to the matching engine.
 */
public record SubmitOrderRequest(

		@NotNull UUID accountId,

		@NotBlank String symbol,

		@NotNull OrderSide side,

		@NotNull @DecimalMin(value = "0.01") BigDecimal price,

		@NotNull @DecimalMin(value = "0.01") BigDecimal quantity

) {
}
