package com.splitflow.infrastructure.adapter.messaging;

import com.splitflow.domain.model.Split;
import com.splitflow.domain.port.out.MessagingGatewayPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RoutingMessagingGatewayAdapter implements MessagingGatewayPort {

    private final ConsoleLoggingMessagingAdapter consoleAdapter;
    private final WhatsAppCloudApiAdapter whatsAppAdapter;

    public RoutingMessagingGatewayAdapter(ConsoleLoggingMessagingAdapter consoleAdapter,
                                         WhatsAppCloudApiAdapter whatsAppAdapter) {
        this.consoleAdapter = consoleAdapter;
        this.whatsAppAdapter = whatsAppAdapter;
    }

    @Override
    public void sendTextMessage(String recipientPhone, String message) {
        consoleAdapter.sendTextMessage(recipientPhone, message);
        if (whatsAppAdapter.isConfigured()) {
            whatsAppAdapter.sendTextMessage(recipientPhone, message);
        }
    }

    @Override
    public void sendGuardrailMenu(String recipientPhone, String reason) {
        consoleAdapter.sendGuardrailMenu(recipientPhone, reason);
        if (whatsAppAdapter.isConfigured()) {
            whatsAppAdapter.sendGuardrailMenu(recipientPhone, reason);
        }
    }

    @Override
    public void sendSplitCard(String debtorPhone, Split split, String creditorName) {
        consoleAdapter.sendSplitCard(debtorPhone, split, creditorName);
        if (whatsAppAdapter.isConfigured()) {
            whatsAppAdapter.sendSplitCard(debtorPhone, split, creditorName);
        }
    }

    @Override
    public void sendClaimVerificationCard(String creditorPhone, Split split, String debtorName) {
        consoleAdapter.sendClaimVerificationCard(creditorPhone, split, debtorName);
        if (whatsAppAdapter.isConfigured()) {
            whatsAppAdapter.sendClaimVerificationCard(creditorPhone, split, debtorName);
        }
    }

    @Override
    public void sendDisputeNotificationToCreditor(String creditorPhone, Split split, String debtorName, String reason) {
        consoleAdapter.sendDisputeNotificationToCreditor(creditorPhone, split, debtorName, reason);
        if (whatsAppAdapter.isConfigured()) {
            whatsAppAdapter.sendDisputeNotificationToCreditor(creditorPhone, split, debtorName, reason);
        }
    }

    @Override
    public void sendWelcomeMessage(String recipientPhone) {
        consoleAdapter.sendWelcomeMessage(recipientPhone);
        if (whatsAppAdapter.isConfigured()) {
            whatsAppAdapter.sendWelcomeMessage(recipientPhone);
        }
    }
}
