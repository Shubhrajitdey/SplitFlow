package com.splitflow.infrastructure.web;

import com.splitflow.domain.model.PairwiseLedger;
import com.splitflow.domain.model.Split;
import com.splitflow.domain.model.User;
import com.splitflow.domain.port.in.ProcessIncomingMessageUseCase;
import com.splitflow.domain.port.out.PairwiseLedgerRepositoryPort;
import com.splitflow.domain.port.out.SplitRepositoryPort;
import com.splitflow.domain.port.out.UserRepositoryPort;
import com.splitflow.infrastructure.adapter.persistence.repository.SpringDataLedgerRepository;
import com.splitflow.infrastructure.adapter.persistence.repository.SpringDataSplitRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test")
public class ApiTestController {

    private final ProcessIncomingMessageUseCase processIncomingMessageUseCase;
    private final UserRepositoryPort userRepository;
    private final SpringDataSplitRepository springDataSplitRepository;
    private final SpringDataLedgerRepository springDataLedgerRepository;

    public ApiTestController(
            ProcessIncomingMessageUseCase processIncomingMessageUseCase,
            UserRepositoryPort userRepository,
            SpringDataSplitRepository springDataSplitRepository,
            SpringDataLedgerRepository springDataLedgerRepository) {
        this.processIncomingMessageUseCase = processIncomingMessageUseCase;
        this.userRepository = userRepository;
        this.springDataSplitRepository = springDataSplitRepository;
        this.springDataLedgerRepository = springDataLedgerRepository;
    }

    public record MessageRequest(String from, String text) {}
    public record ButtonRequest(String from, String buttonId) {}
    public record ContactLinkRequest(String ownerPhone, String alias, String targetPhone) {}

    @PostMapping("/simulate-message")
    public ResponseEntity<Map<String, String>> simulateMessage(@RequestBody MessageRequest req) {
        processIncomingMessageUseCase.processTextMessage(req.from(), req.text());
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Processed incoming message: " + req.text()));
    }

    @PostMapping("/simulate-button")
    public ResponseEntity<Map<String, String>> simulateButton(@RequestBody ButtonRequest req) {
        processIncomingMessageUseCase.processInteractiveButton(req.from(), req.buttonId());
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Processed button: " + req.buttonId()));
    }

    @PostMapping("/simulate-prohibited-media")
    public ResponseEntity<Map<String, String>> simulateProhibitedMedia(@RequestBody Map<String, String> req) {
        processIncomingMessageUseCase.processProhibitedMedia(req.get("from"), req.getOrDefault("mediaType", "image"));
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Guardrail refusal triggered"));
    }

    @PostMapping("/link-contact")
    public ResponseEntity<Map<String, String>> linkContact(@RequestBody ContactLinkRequest req) {
        User owner = userRepository.findByPhoneNumber(req.ownerPhone())
                .orElseGet(() -> userRepository.save(User.createNew(req.ownerPhone(), "User_" + req.ownerPhone())));

        owner.addContactAlias(req.alias(), req.targetPhone());
        userRepository.save(owner);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Linked alias '" + req.alias() + "' -> '" + req.targetPhone() + "'"
        ));
    }

    @GetMapping("/splits")
    public ResponseEntity<List<?>> getAllSplits() {
        return ResponseEntity.ok(springDataSplitRepository.findAll());
    }

    @GetMapping("/ledgers")
    public ResponseEntity<List<?>> getAllLedgers() {
        return ResponseEntity.ok(springDataLedgerRepository.findAll());
    }
}
