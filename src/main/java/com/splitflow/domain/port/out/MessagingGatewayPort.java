package com.splitflow.domain.port.out;

import com.splitflow.domain.model.Split;

public interface MessagingGatewayPort {
    void sendTextMessage(String recipientPhone, String message);
    void sendGuardrailMenu(String recipientPhone, String reason);
    void sendSplitCard(String debtorPhone, Split split, String creditorName);
    void sendClaimVerificationCard(String creditorPhone, Split split, String debtorName);
    void sendDisputeNotificationToCreditor(String creditorPhone, Split split, String debtorName, String reason);
    void sendWelcomeMessage(String recipientPhone);
}
