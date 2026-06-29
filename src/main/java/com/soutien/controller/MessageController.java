package com.soutien.controller;

import com.soutien.dto.MessageCreateRequest;
import com.soutien.dto.MessageResponse;
import com.soutien.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests/{requestId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /** Envoyer un message lie a une demande (eleve ou enseignant affecte uniquement). */
    @PostMapping
    public ResponseEntity<MessageResponse> envoyer(@PathVariable Long requestId,
                                                    @Valid @RequestBody MessageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.envoyerMessage(requestId, request));
    }

    /** Historique des messages d'une demande. */
    @GetMapping
    public List<MessageResponse> historique(@PathVariable Long requestId) {
        return messageService.historique(requestId);
    }
}
