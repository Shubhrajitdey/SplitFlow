package com.splitflow.domain.port.in;

public interface ProcessIncomingMessageUseCase {
    void processTextMessage(String senderPhone, String text);
    void processVoiceNote(String senderPhone, byte[] audioBytes, String mimeType);
    void processInteractiveButton(String senderPhone, String buttonId);
    void processProhibitedMedia(String senderPhone, String mediaType);
}
