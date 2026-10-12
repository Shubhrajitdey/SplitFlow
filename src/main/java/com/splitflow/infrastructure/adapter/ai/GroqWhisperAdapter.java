package com.splitflow.infrastructure.adapter.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Component
public class GroqWhisperAdapter {

    private static final Logger log = LoggerFactory.getLogger(GroqWhisperAdapter.class);

    @Value("${splitflow.ai.groq.api-key:}")
    private String apiKey;

    @Value("${splitflow.ai.groq.whisper-model:whisper-large-v3}")
    private String model;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String transcribe(byte[] audioBytes, String filename) {
        if (!isConfigured() || audioBytes == null || audioBytes.length == 0) {
            log.info("Groq API key not configured, returning empty transcription.");
            return null;
        }

        try {
            String boundary = "---" + UUID.randomUUID();
            byte[] body = buildMultipartBody(boundary, audioBytes, filename != null ? filename : "audio.ogg");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/audio/transcriptions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("text").asText();
            }
        } catch (Exception e) {
            log.warn("Groq Whisper transcription failed: {}", e.getMessage());
        }
        return null;
    }

    private byte[] buildMultipartBody(String boundary, byte[] fileData, String fileName) throws Exception {
        String crlf = "\r\n";
        StringBuilder sb = new StringBuilder();

        // Model param
        sb.append("--").append(boundary).append(crlf);
        sb.append("Content-Disposition: form-data; name=\"model\"").append(crlf).append(crlf);
        sb.append(model).append(crlf);

        // File header
        sb.append("--").append(boundary).append(crlf);
        sb.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"").append(crlf);
        sb.append("Content-Type: audio/ogg").append(crlf).append(crlf);

        byte[] headerBytes = sb.toString().getBytes();
        byte[] footerBytes = (crlf + "--" + boundary + "--" + crlf).getBytes();

        byte[] total = new byte[headerBytes.length + fileData.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, total, 0, headerBytes.length);
        System.arraycopy(fileData, 0, total, headerBytes.length, fileData.length);
        System.arraycopy(footerBytes, 0, total, headerBytes.length + fileData.length, footerBytes.length);

        return total;
    }
}
