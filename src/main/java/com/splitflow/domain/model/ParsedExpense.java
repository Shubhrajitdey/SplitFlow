package com.splitflow.domain.model;

import java.math.BigDecimal;

public record ParsedExpense(
        ExpenseIntent intent,
        BigDecimal amount,
        String counterpartyAlias,
        String userTitle,
        SplitType splitType,
        String rawInput,
        String outOfScopeReason
) {
    public static ParsedExpense outOfScope(String rawInput, String reason) {
        return new ParsedExpense(ExpenseIntent.OUT_OF_SCOPE, null, null, null, null, rawInput, reason);
    }

    public static ParsedExpense logExpense(BigDecimal amount, String counterpartyAlias, String userTitle, SplitType splitType, String rawInput) {
        return new ParsedExpense(ExpenseIntent.LOG_EXPENSE, amount, counterpartyAlias, userTitle, splitType, rawInput, null);
    }

    public static ParsedExpense claimPayment(BigDecimal amount, String counterpartyAlias, String rawInput) {
        return new ParsedExpense(ExpenseIntent.CLAIM_PAYMENT, amount, counterpartyAlias, null, null, rawInput, null);
    }

    public static ParsedExpense checkBalance(String counterpartyAlias, String rawInput) {
        return new ParsedExpense(ExpenseIntent.CHECK_BALANCE, null, counterpartyAlias, null, null, rawInput, null);
    }

    public static ParsedExpense greeting(String rawInput) {
        return new ParsedExpense(ExpenseIntent.GREETING, null, null, null, null, rawInput, null);
    }
}
