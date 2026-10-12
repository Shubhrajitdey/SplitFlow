package com.splitflow.infrastructure.adapter.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitflow.domain.model.Split;
import com.splitflow.domain.port.out.MessagingGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class WhatsAppCloudApiAdapter implements MessagingGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppCloudApiAdapter.class);

    @Value("${splitflow.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${splitflow.whatsapp.api-url:https://graph.facebook.com/v20.0}")
    private String apiUrl;

    @Value("${splitflow.whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${splitflow.whatsapp.api-token:}")
    private String apiToken;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return enabled && apiToken != null && !apiToken.isBlank() && phoneNumberId != null && !phoneNumberId.isBlank();
    }

    @Override
    public void sendTextMessage(String recipientPhone, String message) {
        if (!isConfigured()) return;

        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", cleanPhone(recipientPhone),
                "type", "text",
                "text", Map.of("body", message)
        );
        send(payload);
    }

    @Override
    public void sendGuardrailMenu(String recipientPhone, String reason) {
        if (!isConfigured()) return;
        sendTextMessage(recipientPhone, reason + "\n\nTry:\n• 'Paid 1200 for Pizza with Rahul'\n• 'Paid 500 with Rahul'\n• 'What do I owe?'");
    }

    @Override
    public void sendSplitCard(String debtorPhone, Split split, String creditorName) {
        if (!isConfigured()) return;

        List<Map<String, Object>> buttons = new ArrayList<>();
        buttons.add(buildButton("CLAIM_PAID:" + split.getId(), "🟢 I've Paid"));

        // Single-turn dispute: exclude dispute button if dispute is locked (AC-2)
        if (!split.isDisputeLocked()) {
            buttons.add(buildButton("DISPUTE:" + split.getId(), "❓ Dispute"));
        }
        buttons.add(buildButton("PAY_LATER:" + split.getId(), "⏳ Pay Later"));

        String bodyText = String.format("%s\n%s says you owe: ₹%s",
                split.getDisplayName(), creditorName, split.getRemainingAmount());

        sendInteractiveButtonMessage(debtorPhone, bodyText, buttons);
    }

    @Override
    public void sendClaimVerificationCard(String creditorPhone, Split split, String debtorName) {
        if (!isConfigured()) return;

        List<Map<String, Object>> buttons = List.of(
                buildButton("CONFIRM_PAID:" + split.getId(), "✅ Received"),
                buildButton("REJECT_PAID:" + split.getId(), "❌ Not Received")
        );

        String bodyText = String.format("%s claims they paid ₹%s for '%s'. Did you receive this?",
                debtorName, split.getRemainingAmount(), split.getDisplayName());

        sendInteractiveButtonMessage(creditorPhone, bodyText, buttons);
    }

    @Override
    public void sendDisputeNotificationToCreditor(String creditorPhone, Split split, String debtorName, String reason) {
        if (!isConfigured()) return;

        List<Map<String, Object>> buttons = List.of(
                buildButton("REJECT_DISPUTE:" + split.getId(), "❌ Amount Correct")
        );

        String bodyText = String.format("%s disputed '%s' (₹%s).\nReason: %s\nAuto-reminders are paused.",
                debtorName, split.getDisplayName(), split.getRemainingAmount(), reason);

        sendInteractiveButtonMessage(creditorPhone, bodyText, buttons);
    }

    @Override
    public void sendWelcomeMessage(String recipientPhone) {
        if (!isConfigured()) return;

        String welcomeText = "👋 Welcome to SplitFlow AI!\nI'm your conversational bookkeeper right inside WhatsApp.\n\n" +
                "Try sending text or voice notes like:\n" +
                "• 'Paid 1200 for Pizza Dinner with Rahul'\n" +
                "• 'Paid 500 with Rahul' (Auto funky tag)\n" +
                "• 'Paid 500 to Rahul' (Clear dues)\n" +
                "• 'What do I owe?'";

        List<Map<String, Object>> buttons = List.of(
                buildButton("DEMO_EXAMPLE", "🍕 Try Demo Bill"),
                buildButton("CHECK_DUES", "📊 Check Dues")
        );

        sendInteractiveButtonMessage(recipientPhone, welcomeText, buttons);
    }

    private Map<String, Object> buildButton(String id, String title) {
        return Map.of(
                "type", "reply",
                "reply", Map.of("id", id, "title", title)
        );
    }

    private void sendInteractiveButtonMessage(String to, String body, List<Map<String, Object>> buttons) {
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "to", cleanPhone(to),
                "type", "interactive",
                "interactive", Map.of(
                        "type", "button",
                        "body", Map.of("text", body),
                        "action", Map.of("buttons", buttons)
                )
        );
        send(payload);
    }

    private void send(Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "/" + phoneNumberId + "/messages"))
                    .header("Authorization", "Bearer " + apiToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(6))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                log.warn("Meta WhatsApp API error ({}): {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message: {}", e.getMessage());
        }
    }

    private String cleanPhone(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }
}
