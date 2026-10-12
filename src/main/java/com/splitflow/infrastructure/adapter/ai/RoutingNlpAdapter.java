package com.splitflow.infrastructure.adapter.ai;

import com.splitflow.domain.model.ParsedExpense;
import com.splitflow.domain.port.out.NlpParserPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class RoutingNlpAdapter implements NlpParserPort {

    private final GeminiNlpAdapter geminiAdapter;
    private final RuleBasedNlpAdapter ruleBasedAdapter;
    private final GroqWhisperAdapter whisperAdapter;

    public RoutingNlpAdapter(GeminiNlpAdapter geminiAdapter,
                             RuleBasedNlpAdapter ruleBasedAdapter,
                             GroqWhisperAdapter whisperAdapter) {
        this.geminiAdapter = geminiAdapter;
        this.ruleBasedAdapter = ruleBasedAdapter;
        this.whisperAdapter = whisperAdapter;
    }

    @Override
    public ParsedExpense parseText(String text) {
        if (text == null || text.isBlank()) {
            return ParsedExpense.outOfScope(text, "Message is empty.");
        }

        // Try Gemini if configured
        if (geminiAdapter.isConfigured()) {
            ParsedExpense parsed = geminiAdapter.parse(text);
            if (parsed != null) {
                return parsed;
            }
        }

        // Fall back to rule-based engine
        return ruleBasedAdapter.parse(text);
    }

    @Override
    public ParsedExpense parseAudio(byte[] audioBytes, String mimeType) {
        String transcript = whisperAdapter.transcribe(audioBytes, "voice.ogg");
        if (transcript != null && !transcript.isBlank()) {
            return parseText(transcript);
        }
        return ParsedExpense.outOfScope("Voice Note", "Could not transcribe voice note. Please type the amount and person.");
    }
}
