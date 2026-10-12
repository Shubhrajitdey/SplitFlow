package com.splitflow.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitflow.domain.port.in.ProcessIncomingMessageUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    @Value("${splitflow.whatsapp.verify-token:splitflow_secret_token_123}")
    private String verifyToken;

    private final ProcessIncomingMessageUseCase processIncomingMessageUseCase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WhatsAppWebhookController(ProcessIncomingMessageUseCase processIncomingMessageUseCase) {
        this.processIncomingMessageUseCase = processIncomingMessageUseCase;
    }

    /**
     * Webhook Verification Endpoint (Meta requirement)
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.challenge") String challenge,
            @RequestParam("hub.verify_token") String token) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            log.info("WhatsApp Webhook successfully verified with Meta!");
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch");
    }

    /**
     * Webhook Event Ingestion Endpoint
     */
    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode entries = root.path("entry");
            if (entries.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            JsonNode changes = entries.get(0).path("changes");
            if (changes.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            JsonNode value = changes.get(0).path("value");
            JsonNode messages = value.path("messages");

            if (messages.isArray() && !messages.isEmpty()) {
                JsonNode msg = messages.get(0);
                String from = msg.path("from").asText();
                String type = msg.path("type").asText();

                switch (type) {
                    case "text" -> {
                        String text = msg.path("text").path("body").asText();
                        processIncomingMessageUseCase.processTextMessage(from, text);
                    }
                    case "interactive" -> {
                        String buttonId = msg.path("interactive").path("button_reply").path("id").asText();
                        processIncomingMessageUseCase.processInteractiveButton(from, buttonId);
                    }
                    case "image", "document" -> {
                        processIncomingMessageUseCase.processProhibitedMedia(from, type);
                    }
                    default -> {
                        log.info("Unhandled WhatsApp message type: {}", type);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error parsing WhatsApp webhook payload: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }
}
