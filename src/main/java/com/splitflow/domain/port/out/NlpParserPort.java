package com.splitflow.domain.port.out;

import com.splitflow.domain.model.ParsedExpense;

public interface NlpParserPort {
    ParsedExpense parseText(String text);
    ParsedExpense parseAudio(byte[] audioBytes, String mimeType);
}
