package com.soutien.dto;

import com.soutien.entity.RequestStatus;
import com.soutien.entity.SupportRequest;

import java.time.LocalDateTime;

public record SupportRequestResponse(
        Long id,
        Long eleveId,
        String eleveNom,
        Long subjectId,
        String subjectNom,
        Long enseignantId,
        String enseignantNom,
        String description,
        RequestStatus status,
        LocalDateTime dateCreation,
        LocalDateTime dateMiseAJour
) {
    public static SupportRequestResponse fromEntity(SupportRequest r) {
        return new SupportRequestResponse(
                r.getId(),
                r.getEleve().getId(),
                r.getEleve().getNom(),
                r.getSubject().getId(),
                r.getSubject().getNom(),
                r.getEnseignant() != null ? r.getEnseignant().getId() : null,
                r.getEnseignant() != null ? r.getEnseignant().getNom() : null,
                r.getDescription(),
                r.getStatus(),
                r.getDateCreation(),
                r.getDateMiseAJour()
        );
    }
}
