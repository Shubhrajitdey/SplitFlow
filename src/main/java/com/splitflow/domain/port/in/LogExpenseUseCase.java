package com.splitflow.domain.port.in;

import com.splitflow.domain.model.ParsedExpense;
import com.splitflow.domain.model.Split;

public interface LogExpenseUseCase {
    Split logExpense(String senderPhone, ParsedExpense parsedExpense);
}
