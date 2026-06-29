package com.soutien.dto;

import com.soutien.entity.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long supportRequestId,
        Long auteurId,
        String auteurNom,
        String contenu,
        LocalDateTime dateEnvoi
) {
    public static MessageResponse fromEntity(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getSupportRequest().getId(),
                m.getAuteur().getId(),
                m.getAuteur().getNom(),
                m.getContenu(),
                m.getDateEnvoi()
        );
    }
}
