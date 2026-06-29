package com.soutien.controller;

import com.soutien.dto.StatusUpdateRequest;
import com.soutien.dto.SupportRequestCreateRequest;
import com.soutien.dto.SupportRequestResponse;
import com.soutien.service.SupportRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class SupportRequestController {

    private final SupportRequestService supportRequestService;

    /** Un eleve cree une demande de soutien. */
    @PostMapping
    @PreAuthorize("hasRole('ELEVE')")
    public ResponseEntity<SupportRequestResponse> creer(@Valid @RequestBody SupportRequestCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportRequestService.creerDemande(request));
    }

    /** Demandes disponibles (status=CREEE, non affectees) pour un enseignant. */
    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('ENSEIGNANT','ADMIN')")
    public List<SupportRequestResponse> disponibles() {
        return supportRequestService.demandesDisponibles();
    }

    /** L'enseignant connecte s'affecte a une demande disponible. */
    @PostMapping("/{id}/affecter")
    @PreAuthorize("hasRole('ENSEIGNANT')")
    public SupportRequestResponse affecter(@PathVariable Long id) {
        return supportRequestService.affecterEnseignant(id);
    }

    /** Changement de statut d'une demande (createur, enseignant affecte ou admin). */
    @PatchMapping("/{id}/statut")
    public SupportRequestResponse changerStatut(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return supportRequestService.changerStatut(id, request.status());
    }

    /** Mes demandes : eleve -> ses demandes, enseignant -> ses demandes affectees, admin -> toutes. */
    @GetMapping("/mes-demandes")
    public List<SupportRequestResponse> mesDemandes() {
        return supportRequestService.mesPemandes();
    }

    @GetMapping("/{id}")
    public SupportRequestResponse findById(@PathVariable Long id) {
        return supportRequestService.findById(id);
    }
}
