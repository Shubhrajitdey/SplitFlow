package com.splitflow.infrastructure.adapter.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitflow.domain.model.ExpenseIntent;
import com.splitflow.domain.model.ParsedExpense;
import com.splitflow.domain.model.SplitType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class GeminiNlpAdapter {

    private static final Logger log = LoggerFactory.getLogger(GeminiNlpAdapter.class);

    @Value("${splitflow.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${splitflow.ai.gemini.model:gemini-1.5-flash}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public ParsedExpense parse(String userText) {
        if (!isConfigured()) {
            return null;
        }

        try {
            String prompt = """
                    You are SplitFlow AI's strict financial classifier.
                    Parse the following user message into a JSON object:
                    User input: "%s"

                    Rules:
                    1. intent MUST be one of: LOG_EXPENSE, CLAIM_PAYMENT, CHECK_BALANCE, OUT_OF_SCOPE.
                    2. If user talks about receipts/photos, chit-chat, weather, or random talk, intent is OUT_OF_SCOPE.
                    3. For LOG_EXPENSE: extract amount (numeric), counterpartyAlias (person name), userTitle (optional title, null if omitted), splitType ("EQUAL_SPLIT" if "with person", "FULL_AMOUNT" if "for person").
                    4. For CLAIM_PAYMENT: extract amount and counterpartyAlias.
                    5. Output ONLY valid JSON:
                    {
                      "intent": "LOG_EXPENSE",
                      "amount": 1200,
                      "counterpartyAlias": "Rahul",
                      "userTitle": "Pizza Dinner",
                      "splitType": "EQUAL_SPLIT",
                      "outOfScopeReason": null
                    }
                    """.formatted(userText.replace("\"", "\\\""));

            String requestBody = """
                    {
                      "contents": [{
                        "parts": [{"text": %s}]
                      }],
                      "generationConfig": {
                        "response_mime_type": "application/json"
                      }
                    }
                    """.formatted(objectMapper.writeValueAsString(prompt));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String candidateText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                JsonNode parsedJson = objectMapper.readTree(candidateText);

                ExpenseIntent intent = ExpenseIntent.valueOf(parsedJson.path("intent").asText("OUT_OF_SCOPE"));
                if (intent == ExpenseIntent.OUT_OF_SCOPE) {
                    return ParsedExpense.outOfScope(userText, parsedJson.path("outOfScopeReason").asText("Out of scope"));
                }

                BigDecimal amount = parsedJson.hasNonNull("amount") ? new BigDecimal(parsedJson.path("amount").asText()) : null;
                String counterparty = parsedJson.hasNonNull("counterpartyAlias") ? parsedJson.path("counterpartyAlias").asText() : null;
                String title = parsedJson.hasNonNull("userTitle") ? parsedJson.path("userTitle").asText() : null;
                SplitType splitType = parsedJson.hasNonNull("splitType") ? SplitType.valueOf(parsedJson.path("splitType").asText()) : SplitType.EQUAL_SPLIT;

                return new ParsedExpense(intent, amount, counterparty, title, splitType, userText, null);
            }
        } catch (Exception e) {
            log.warn("Gemini parsing failed, falling back to rule-based engine: {}", e.getMessage());
        }
        return null;
    }
}
