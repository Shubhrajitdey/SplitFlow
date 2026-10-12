package com.splitflow.infrastructure.adapter.messaging;

import com.splitflow.domain.model.Split;
import com.splitflow.domain.port.out.MessagingGatewayPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ConsoleLoggingMessagingAdapter implements MessagingGatewayPort {

    private static final Logger log = LoggerFactory.getLogger(ConsoleLoggingMessagingAdapter.class);

    @Override
    public void sendTextMessage(String recipientPhone, String message) {
        log.info("""
                
                ================ [ WHATSAPP TEXT MESSAGE ] ================
                To: {}
                Message:
                {}
                ===========================================================
                """, recipientPhone, message);
    }

    @Override
    public void sendGuardrailMenu(String recipientPhone, String reason) {
        log.info("""
                
                ================ [ WHATSAPP GUARDRAIL MENU ] =============
                To: {}
                Notice: {}
                Supported Commands:
                • "Paid 1200 for Pizza Dinner with Rahul"
                • "Paid 500 with Rahul" (Auto funky tag)
                • "Paid 500 to Rahul" (Clear dues)
                • "What do I owe?" (Check balances)
                ==========================================================
                """, recipientPhone, reason);
    }

    @Override
    public void sendSplitCard(String debtorPhone, Split split, String creditorName) {
        StringBuilder buttons = new StringBuilder("[ 🟢 I've Paid ]");
        if (!split.isDisputeLocked()) {
            buttons.append("  [ ❓ Dispute ]");
        }
        buttons.append("  [ ⏳ Pay Later ]");

        log.info("""
                
                ================ [ WHATSAPP INTERACTIVE CARD ] ===========
                To Debtor: {}
                Header: Split Request
                Bill: {}
                Creditor: {}
                Your Share: ₹{}
                Technical ID: {}
                Interactive Buttons:
                {}
                ==========================================================
                """, debtorPhone, split.getDisplayName(), creditorName, split.getRemainingAmount(), split.getId(), buttons);
    }

    @Override
    public void sendClaimVerificationCard(String creditorPhone, Split split, String debtorName) {
        log.info("""
                
                ================ [ WHATSAPP VERIFICATION CARD ] ==========
                To Creditor: {}
                Notice: Payment Verification Needed
                Details: {} claims they paid ₹{} for '{}'.
                Prompt: Did you receive this in your bank / UPI?
                Interactive Buttons:
                [ ✅ Confirm Received ]   [ ❌ Not Received ]
                Payload ID: {}
                ==========================================================
                """, creditorPhone, debtorName, split.getRemainingAmount(), split.getDisplayName(), split.getId());
    }

    @Override
    public void sendDisputeNotificationToCreditor(String creditorPhone, Split split, String debtorName, String reason) {
        log.info("""
                
                ================ [ WHATSAPP DISPUTE NOTIFICATION ] =======
                To Creditor: {}
                Notice: Bill Disputed
                Details: {} has disputed '{}' (₹{}).
                Reason: {}
                Reminder: Auto-reminders are paused.
                Interactive Buttons:
                [ ✏️ Correct Amount ]   [ ❌ Amount Is Correct ]
                Payload ID: {}
                ==========================================================
                """, creditorPhone, debtorName, split.getDisplayName(), split.getRemainingAmount(), reason, split.getId());
    }

    @Override
    public void sendWelcomeMessage(String recipientPhone) {
        log.info("""
                
                ================ [ WHATSAPP ONBOARDING & WELCOME ] =======
                To: {}
                Message:
                👋 Welcome to SplitFlow AI!
                I'm your conversational bookkeeper inside WhatsApp.
                
                🚀 How to use (just type or send a voice note):
                1️⃣ Log a bill:
                   "Paid 1200 for Pizza Dinner with Rahul"
                   (Or "Paid 1200 with Rahul" for an auto funky tag 🍕)
                
                2️⃣ Settle a debt:
                   "Paid 500 to Rahul"
                
                3️⃣ Check balances:
                   "What do I owe?"
                
                Interactive Buttons:
                [ 🍕 Example Bill ]   [ 📊 Check Dues ]
                ==========================================================
                """, recipientPhone);
    }
}
