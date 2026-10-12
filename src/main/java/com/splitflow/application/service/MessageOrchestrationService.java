package com.splitflow.application.service;

import com.splitflow.domain.model.ExpenseIntent;
import com.splitflow.domain.model.ParsedExpense;
import com.splitflow.domain.port.in.ClaimPaymentUseCase;
import com.splitflow.domain.port.in.ConfirmSettlementUseCase;
import com.splitflow.domain.port.in.DisputeSplitUseCase;
import com.splitflow.domain.port.in.LogExpenseUseCase;
import com.splitflow.domain.port.in.ProcessIncomingMessageUseCase;
import com.splitflow.domain.port.in.SettleBalanceUseCase;
import com.splitflow.domain.port.out.MessagingGatewayPort;
import com.splitflow.domain.port.out.NlpParserPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MessageOrchestrationService implements ProcessIncomingMessageUseCase {

    private final LogExpenseUseCase logExpenseUseCase;
    private final ClaimPaymentUseCase claimPaymentUseCase;
    private final ConfirmSettlementUseCase confirmSettlementUseCase;
    private final DisputeSplitUseCase disputeSplitUseCase;
    private final SettleBalanceUseCase settleBalanceUseCase;
    private final NlpParserPort nlpParser;
    private final MessagingGatewayPort messagingGateway;

    public MessageOrchestrationService(
            LogExpenseUseCase logExpenseUseCase,
            ClaimPaymentUseCase claimPaymentUseCase,
            ConfirmSettlementUseCase confirmSettlementUseCase,
            DisputeSplitUseCase disputeSplitUseCase,
            SettleBalanceUseCase settleBalanceUseCase,
            NlpParserPort nlpParser,
            MessagingGatewayPort messagingGateway) {
        this.logExpenseUseCase = logExpenseUseCase;
        this.claimPaymentUseCase = claimPaymentUseCase;
        this.confirmSettlementUseCase = confirmSettlementUseCase;
        this.disputeSplitUseCase = disputeSplitUseCase;
        this.settleBalanceUseCase = settleBalanceUseCase;
        this.nlpParser = nlpParser;
        this.messagingGateway = messagingGateway;
    }

    @Override
    public void processTextMessage(String senderPhone, String text) {
        ParsedExpense parsed = nlpParser.parseText(text);
        routeParsedExpense(senderPhone, parsed);
    }

    @Override
    public void processVoiceNote(String senderPhone, byte[] audioBytes, String mimeType) {
        ParsedExpense parsed = nlpParser.parseAudio(audioBytes, mimeType);
        routeParsedExpense(senderPhone, parsed);
    }

    private void routeParsedExpense(String senderPhone, ParsedExpense parsed) {
        if (parsed.intent() == ExpenseIntent.OUT_OF_SCOPE) {
            // Feature 1.3 Guardrail Refusal (AC-5)
            messagingGateway.sendGuardrailMenu(senderPhone, parsed.outOfScopeReason());
            return;
        }

        switch (parsed.intent()) {
            case GREETING -> {
                messagingGateway.sendWelcomeMessage(senderPhone);
            }
            case LOG_EXPENSE -> {
                if (parsed.counterpartyAlias() == null || parsed.amount() == null) {
                    messagingGateway.sendTextMessage(senderPhone, "Please mention who you split this with and the amount. E.g. 'Paid 500 for lunch with Rahul'");
                    return;
                }
                logExpenseUseCase.logExpense(senderPhone, parsed);
            }
            case CLAIM_PAYMENT -> {
                if (parsed.amount() != null && parsed.counterpartyAlias() != null) {
                    settleBalanceUseCase.settleWithFifo(senderPhone, parsed.counterpartyAlias(), parsed.amount());
                } else {
                    messagingGateway.sendTextMessage(senderPhone, "To record a payment, try: 'Paid 500 to Rahul'");
                }
            }
            case CHECK_BALANCE -> {
                messagingGateway.sendTextMessage(senderPhone, "Checking your balances... (All active dues are shown in your recent ledger cards).");
            }
            default -> messagingGateway.sendGuardrailMenu(senderPhone, "Unsupported operation.");
        }
    }

    @Override
    public void processInteractiveButton(String senderPhone, String buttonId) {
        if (buttonId == null) {
            messagingGateway.sendTextMessage(senderPhone, "Invalid action button.");
            return;
        }

        if ("DEMO_EXAMPLE".equals(buttonId)) {
            messagingGateway.sendTextMessage(senderPhone, "Here's an example you can copy & send right now:\n\n'Paid 1200 for Pizza Dinner with Rahul'");
            return;
        }
        if ("CHECK_DUES".equals(buttonId)) {
            messagingGateway.sendTextMessage(senderPhone, "Checking your open balances... (All active dues are displayed in your recent ledger cards).");
            return;
        }

        if (!buttonId.contains(":")) {
            messagingGateway.sendTextMessage(senderPhone, "Invalid action button.");
            return;
        }

        String[] parts = buttonId.split(":");
        String action = parts[0];
        UUID splitId = UUID.fromString(parts[1]);

        switch (action) {
            case "CLAIM_PAID" -> claimPaymentUseCase.claimPayment(splitId, senderPhone);
            case "CONFIRM_PAID" -> confirmSettlementUseCase.confirmSettlement(splitId, senderPhone, true);
            case "REJECT_PAID" -> confirmSettlementUseCase.confirmSettlement(splitId, senderPhone, false);
            case "DISPUTE" -> disputeSplitUseCase.disputeSplit(splitId, senderPhone, "Amount disputed by debtor");
            case "PAY_LATER" -> messagingGateway.sendTextMessage(senderPhone, "No problem! We'll keep this pending on your ledger.");
            default -> messagingGateway.sendTextMessage(senderPhone, "Action not recognized.");
        }
    }

    @Override
    public void processProhibitedMedia(String senderPhone, String mediaType) {
        // Feature 1.3: Immediate refusal of image/receipt uploads (AC-5)
        messagingGateway.sendTextMessage(senderPhone,
                "Receipt scanning is coming in a future update! For now, please type or voice-record the amount and person.\n" +
                "Try: 'Paid 300 for lunch with Rahul' or 'What do I owe?'");
    }
}
