package com.aiconomy.market.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * End-to-end test for the market REST API with mocked ledger settlement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class OrderControllerIntegrationTest {

	private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

	private static final MockWebServer LEDGER_SERVER = new MockWebServer();

	static {
		try {
			LEDGER_SERVER.start();
		}
		catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	@Container
	@SuppressWarnings("resource")
	static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

	@LocalServerPort
	private int port;

	@Autowired
	private StringRedisTemplate redisTemplate;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
		registry.add("aiconomy.ledger.base-url", () -> LEDGER_SERVER.url("/").toString());
	}

	@AfterAll
	static void shutdownLedgerServer() throws IOException {
		LEDGER_SERVER.shutdown();
	}

	@BeforeEach
	void clearRedis() {
		redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
	}

	@Test
	void submitMatchingOrdersExecutesTradeAndSettlesWithLedger() throws Exception {
		UUID seller = UUID.randomUUID();
		UUID buyer = UUID.randomUUID();

		LEDGER_SERVER.enqueue(new MockResponse().setResponseCode(204));

		String baseUrl = "http://localhost:" + port;

		HttpResponse<String> sellResponse = post(baseUrl + "/api/v1/orders", """
				{
				  "accountId": "%s",
				  "symbol": "WIDGET",
				  "side": "SELL",
				  "price": 10.00,
				  "quantity": 5.00
				}
				""".formatted(seller));

		assertThat(sellResponse.statusCode()).isEqualTo(200);
		assertThat(sellResponse.body()).contains("\"trades\":[]");

		HttpResponse<String> buyResponse = post(baseUrl + "/api/v1/orders", """
				{
				  "accountId": "%s",
				  "symbol": "WIDGET",
				  "side": "BUY",
				  "price": 12.00,
				  "quantity": 3.00
				}
				""".formatted(buyer));

		assertThat(buyResponse.statusCode()).isEqualTo(200);
		assertThat(buyResponse.body()).contains("\"price\":10.00");
		assertThat(buyResponse.body()).contains("\"quantity\":3.00");
		assertThat(buyResponse.body()).contains("\"settlementAmount\":30.00");

		RecordedRequest settlement = LEDGER_SERVER.takeRequest();
		assertThat(settlement.getPath()).isEqualTo("/api/v1/transfers");
		assertThat(settlement.getBody().readUtf8()).contains("\"amount\":30.00");

		HttpResponse<String> top = get(baseUrl + "/api/v1/market/WIDGET/top");
		assertThat(top.body()).contains("\"bestAsk\":10.00");
	}

	@Test
	void ledgerRejectionSurfacesAs422() throws Exception {
		UUID seller = UUID.randomUUID();
		UUID buyer = UUID.randomUUID();
		String baseUrl = "http://localhost:" + port;

		post(baseUrl + "/api/v1/orders", """
				{
				  "accountId": "%s",
				  "symbol": "WIDGET",
				  "side": "SELL",
				  "price": 10.00,
				  "quantity": 1.00
				}
				""".formatted(seller));

		LEDGER_SERVER.enqueue(new MockResponse()
			.setResponseCode(422)
			.setBody("{\"error\":\"Insufficient funds\"}"));

		HttpResponse<String> buyResponse = post(baseUrl + "/api/v1/orders", """
				{
				  "accountId": "%s",
				  "symbol": "WIDGET",
				  "side": "BUY",
				  "price": 10.00,
				  "quantity": 1.00
				}
				""".formatted(buyer));

		assertThat(buyResponse.statusCode()).isEqualTo(422);
		assertThat(buyResponse.body()).contains("Insufficient funds");
	}

	private HttpResponse<String> post(String url, String json) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(json))
			.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> get(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.GET()
			.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

}
