package com.aiconomy.ledger.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration test for the ledger REST API using Java HttpClient.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class AccountControllerIntegrationTest {

	private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_IMAGE);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@Test
	void createAccountTransferAndFetchBalance() throws Exception {
		String baseUrl = "http://localhost:" + port;

		HttpResponse<String> createResponse = post(baseUrl + "/api/v1/accounts", """
				{
				  "ownerId": "consumer-api",
				  "accountType": "CONSUMER",
				  "initialBalance": 200.00
				}
				""");

		assertThat(createResponse.statusCode()).isEqualTo(201);
		String sourceId = extractJsonField(createResponse.body(), "id");

		HttpResponse<String> destResponse = post(baseUrl + "/api/v1/accounts", """
				{
				  "ownerId": "firm-api",
				  "accountType": "FIRM",
				  "initialBalance": 0.00
				}
				""");

		String destId = extractJsonField(destResponse.body(), "id");

		HttpResponse<String> transferResponse = post(baseUrl + "/api/v1/transfers", """
				{
				  "fromAccountId": "%s",
				  "toAccountId": "%s",
				  "amount": 45.00
				}
				""".formatted(sourceId, destId));

		assertThat(transferResponse.statusCode()).isEqualTo(204);

		HttpResponse<String> sourceBalance = get(baseUrl + "/api/v1/accounts/" + sourceId);
		HttpResponse<String> destBalance = get(baseUrl + "/api/v1/accounts/" + destId);

		assertThat(extractJsonField(sourceBalance.body(), "balance")).isEqualTo("155.00");
		assertThat(extractJsonField(destBalance.body(), "balance")).isEqualTo("45.00");
	}

	@Test
	void getMissingAccountReturns404() throws Exception {
		UUID missingId = UUID.randomUUID();
		HttpResponse<String> response = get("http://localhost:" + port + "/api/v1/accounts/" + missingId);
		assertThat(response.statusCode()).isEqualTo(404);
	}

	@Test
	void malformedJsonReturns400WithErrorBody() throws Exception {
		HttpResponse<String> response = post("http://localhost:" + port + "/api/v1/accounts", "{ not json");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).isEqualTo("{\"error\":\"Malformed JSON request body\"}");
	}

	@Test
	void validationFailureReturns400WithFieldError() throws Exception {
		HttpResponse<String> response = post("http://localhost:" + port + "/api/v1/accounts", """
				{
				  "ownerId": "",
				  "accountType": "CONSUMER",
				  "initialBalance": 10.00
				}
				""");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.body()).contains("\"error\":\"ownerId:");
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

	private String extractJsonField(String json, String field) {
		String pattern = "\"" + field + "\":\"";
		int start = json.indexOf(pattern);
		if (start >= 0) {
			start += pattern.length();
			int end = json.indexOf('"', start);
			return json.substring(start, end);
		}
		pattern = "\"" + field + "\":";
		start = json.indexOf(pattern);
		start += pattern.length();
		int end = json.indexOf(',', start);
		if (end < 0) {
			end = json.indexOf('}', start);
		}
		return json.substring(start, end).trim().replace("\"", "");
	}

}
