package com.aiconomy.market.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.aiconomy.market.domain.Trade;
import com.aiconomy.market.service.exception.SettlementFailedException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class LedgerSettlementClientTest {

	private MockWebServer ledgerServer;

	private LedgerSettlementClient client;

	@BeforeEach
	void setUp() throws IOException {
		ledgerServer = new MockWebServer();
		ledgerServer.start();
		client = new LedgerSettlementClient(RestClient.builder().baseUrl(ledgerServer.url("/").toString()).build());
	}

	@AfterEach
	void tearDown() throws IOException {
		ledgerServer.shutdown();
	}

	@Test
	void postsTransferWithSettlementAmount() throws InterruptedException {
		ledgerServer.enqueue(new MockResponse().setResponseCode(204));

		Trade trade = new Trade(
				UUID.randomUUID(),
				"WIDGET",
				new BigDecimal("10.00"),
				new BigDecimal("3.00"),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				Instant.parse("2026-06-30T12:00:00Z"));

		client.settle(trade);

		RecordedRequest request = ledgerServer.takeRequest();
		assertThat(request.getMethod()).isEqualTo("POST");
		assertThat(request.getPath()).isEqualTo("/api/v1/transfers");
		String body = request.getBody().readUtf8();
		assertThat(body).contains("\"amount\":30");
		assertThat(body).contains("11111111-1111-1111-1111-111111111111");
	}

	@Test
	void mapsLedgerClientErrorsToSettlementFailedException() {
		ledgerServer.enqueue(new MockResponse()
			.setResponseCode(422)
			.setBody("{\"error\":\"Insufficient funds\"}"));

		Trade trade = new Trade(
				UUID.randomUUID(),
				"WIDGET",
				new BigDecimal("5.00"),
				new BigDecimal("1.00"),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				Instant.now());

		assertThatThrownBy(() -> client.settle(trade))
			.isInstanceOf(SettlementFailedException.class)
			.satisfies(ex -> assertThat(((SettlementFailedException) ex).getStatusCode().value()).isEqualTo(422));
	}

}
