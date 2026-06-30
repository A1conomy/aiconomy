package com.aiconomy.market.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiconomy.market.dto.BookTopResponse;
import com.aiconomy.market.dto.SubmitOrderRequest;
import com.aiconomy.market.dto.SubmitOrderResponse;
import com.aiconomy.market.service.OrderBookStore;
import com.aiconomy.market.service.OrderService;

import jakarta.validation.Valid;

/**
 * REST API for the Open Market Matching Engine.
 */
@RestController
@RequestMapping("/api/v1")
public class OrderController {

	private final OrderService orderService;

	private final OrderBookStore orderBookStore;

	public OrderController(OrderService orderService, OrderBookStore orderBookStore) {
		this.orderService = orderService;
		this.orderBookStore = orderBookStore;
	}

	@PostMapping("/orders")
	public SubmitOrderResponse submitOrder(@Valid @RequestBody SubmitOrderRequest request) {
		return orderService.submitOrder(request);
	}

	@GetMapping("/market/{symbol}/top")
	public BookTopResponse getTopOfBook(@PathVariable String symbol) {
		return new BookTopResponse(
				symbol,
				orderBookStore.getBestBidPrice(symbol).orElse(null),
				orderBookStore.getBestAskPrice(symbol).orElse(null));
	}

}
