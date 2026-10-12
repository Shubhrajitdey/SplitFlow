package com.splitflow.infrastructure.adapter.ai;

import com.splitflow.domain.model.ExpenseIntent;
import com.splitflow.domain.model.ParsedExpense;
import com.splitflow.domain.model.SplitType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedNlpAdapter {

    // Regex: "Paid 1200 for Pizza Dinner with Rahul" or "Paid 500 with Rahul"
    private static final Pattern EXPENSE_WITH_TITLE_PATTERN = Pattern.compile(
            "(?i)paid\\s+(\\d+(?:\\.\\d+)?)\\s+for\\s+(.+?)\\s+with\\s+@?([a-zA-Z0-9_+]+)"
    );

    private static final Pattern EXPENSE_WITHOUT_TITLE_PATTERN = Pattern.compile(
            "(?i)paid\\s+(\\d+(?:\\.\\d+)?)\\s+with\\s+@?([a-zA-Z0-9_+]+)"
    );

    // Regex: "Paid 500 for Rahul" or "Lent 500 to Rahul" (Full debt)
    private static final Pattern FULL_DEBT_PATTERN = Pattern.compile(
            "(?i)(?:paid|lent)\\s+(\\d+(?:\\.\\d+)?)\\s+(?:for|to)\\s+@?([a-zA-Z0-9_+]+)"
    );

    // Regex: Balance check: "What do I owe?" or "Check balance"
    private static final Pattern BALANCE_CHECK_PATTERN = Pattern.compile(
            "(?i).*(what do i owe|balance|dues|how much do i owe).*"
    );

    // Regex: Greetings & Help: "Hi", "Hello", "Hey", "Start", "Help"
    private static final Pattern GREETING_PATTERN = Pattern.compile(
            "(?i)^(hi|hello|hey|start|menu|help|splitflow).*"
    );

    public ParsedExpense parse(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return ParsedExpense.outOfScope(rawText, "Empty message");
        }

        String trimmed = rawText.trim();

        // 0. Greeting & onboarding request
        if (GREETING_PATTERN.matcher(trimmed).matches()) {
            return ParsedExpense.greeting(trimmed);
        }

        // 1. Balance check
        if (BALANCE_CHECK_PATTERN.matcher(trimmed).matches()) {
            return ParsedExpense.checkBalance(null, trimmed);
        }

        // 2. Expense with custom title: "Paid 1200 for Pizza Dinner with Rahul"
        Matcher m1 = EXPENSE_WITH_TITLE_PATTERN.matcher(trimmed);
        if (m1.find()) {
            BigDecimal amount = new BigDecimal(m1.group(1));
            String title = m1.group(2).trim();
            String counterparty = m1.group(3).trim();
            return ParsedExpense.logExpense(amount, counterparty, title, SplitType.EQUAL_SPLIT, trimmed);
        }

        // 3. Expense without custom title: "Paid 1200 with Rahul" (Triggers Autonomous Funky Generator)
        Matcher m2 = EXPENSE_WITHOUT_TITLE_PATTERN.matcher(trimmed);
        if (m2.find()) {
            BigDecimal amount = new BigDecimal(m2.group(1));
            String counterparty = m2.group(2).trim();
            return ParsedExpense.logExpense(amount, counterparty, null, SplitType.EQUAL_SPLIT, trimmed);
        }

        // 4. Direct settlement claim or full debt: "Paid 500 to Rahul"
        Matcher m3 = FULL_DEBT_PATTERN.matcher(trimmed);
        if (m3.find()) {
            BigDecimal amount = new BigDecimal(m3.group(1));
            String counterparty = m3.group(2).trim();
            if (trimmed.toLowerCase().startsWith("paid") && trimmed.toLowerCase().contains(" to ")) {
                return ParsedExpense.claimPayment(amount, counterparty, trimmed);
            } else {
                return ParsedExpense.logExpense(amount, counterparty, null, SplitType.FULL_AMOUNT, trimmed);
            }
        }

        // 5. Out of scope / General chit-chat guardrail (Feature 1.3 / AC-5)
        return ParsedExpense.outOfScope(trimmed, "I can only help you split bills, track debts, and confirm payments.");
    }
}
