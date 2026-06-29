package com.soutien.service;

import com.soutien.dto.SupportRequestCreateRequest;
import com.soutien.dto.SupportRequestResponse;
import com.soutien.entity.*;
import com.soutien.exception.BusinessRuleException;
import com.soutien.exception.ResourceNotFoundException;
import com.soutien.repository.SupportRequestRepository;
import com.soutien.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final UserRepository userRepository;
    private final SubjectService subjectService;
    private final SecurityUtils securityUtils;

    // Transitions de statut autorisees
    private static final Set<RequestStatus> FROM_CREEE = Set.of(RequestStatus.EN_COURS, RequestStatus.ANNULEE);
    private static final Set<RequestStatus> FROM_EN_COURS = Set.of(RequestStatus.TERMINEE, RequestStatus.ANNULEE);

    /** Un eleve cree une demande de soutien dans une matiere. */
    public SupportRequestResponse creerDemande(SupportRequestCreateRequest request) {
        User eleve = securityUtils.getCurrentUser();
        if (eleve.getRole() != Role.ELEVE) {
            throw new BusinessRuleException("Seul un eleve peut creer une demande de soutien.");
        }

        Subject subject = subjectService.getEntityOrThrow(request.subjectId());

        SupportRequest demande = SupportRequest.builder()
                .eleve(eleve)
                .subject(subject)
                .description(request.description())
                .status(RequestStatus.CREEE)
                .build();

        return SupportRequestResponse.fromEntity(supportRequestRepository.save(demande));
    }

    /** Un enseignant consulte les demandes disponibles (status=CREEE, pas encore affectees). */
    public List<SupportRequestResponse> demandesDisponibles() {
        User user = securityUtils.getCurrentUser();
        if (user.getRole() != Role.ENSEIGNANT && user.getRole() != Role.ADMIN) {
            throw new BusinessRuleException("Seul un enseignant ou un administrateur peut consulter les demandes disponibles.");
        }
        return supportRequestRepository.findByStatusAndEnseignantIsNull(RequestStatus.CREEE)
                .stream().map(SupportRequestResponse::fromEntity).toList();
    }

    /** Affecte l'enseignant connecte a une demande disponible. */
    public SupportRequestResponse affecterEnseignant(Long requestId) {
        User enseignant = securityUtils.getCurrentUser();
        if (enseignant.getRole() != Role.ENSEIGNANT) {
            throw new BusinessRuleException("Seul un enseignant peut s'affecter a une demande.");
        }

        SupportRequest demande = getEntityOrThrow(requestId);

        if (demande.getEnseignant() != null) {
            throw new BusinessRuleException("Cette demande est deja affectee a un enseignant.");
        }
        if (demande.getStatus() != RequestStatus.CREEE) {
            throw new BusinessRuleException("Seule une demande au statut CREEE peut etre affectee.");
        }

        demande.setEnseignant(enseignant);
        demande.setStatus(RequestStatus.EN_COURS);
        demande.setDateMiseAJour(LocalDateTime.now());

        return SupportRequestResponse.fromEntity(supportRequestRepository.save(demande));
    }

    /** Changement de statut avec controle des transitions valides et des droits. */
    public SupportRequestResponse changerStatut(Long requestId, RequestStatus nouveauStatut) {
        User user = securityUtils.getCurrentUser();
        SupportRequest demande = getEntityOrThrow(requestId);

        verifierDroitsSurDemande(user, demande);
        verifierTransition(demande.getStatus(), nouveauStatut);

        demande.setStatus(nouveauStatut);
        demande.setDateMiseAJour(LocalDateTime.now());

        return SupportRequestResponse.fromEntity(supportRequestRepository.save(demande));
    }

    private void verifierTransition(RequestStatus actuel, RequestStatus cible) {
        boolean valide = switch (actuel) {
            case CREEE -> FROM_CREEE.contains(cible);
            case EN_COURS -> FROM_EN_COURS.contains(cible);
            case TERMINEE, ANNULEE -> false; // statuts finaux, aucune transition possible
        };
        if (!valide) {
            throw new BusinessRuleException(
                    "Transition de statut invalide : " + actuel + " -> " + cible);
        }
    }

    /** Liste les demandes visibles par l'utilisateur connecte selon son role. */
    public List<SupportRequestResponse> mesPemandes() {
        User user = securityUtils.getCurrentUser();
        List<SupportRequest> demandes = switch (user.getRole()) {
            case ELEVE -> supportRequestRepository.findByEleveId(user.getId());
            case ENSEIGNANT -> supportRequestRepository.findByEnseignantId(user.getId());
            case ADMIN -> supportRequestRepository.findAll();
        };
        return demandes.stream().map(SupportRequestResponse::fromEntity).toList();
    }

    public SupportRequestResponse findById(Long id) {
        User user = securityUtils.getCurrentUser();
        SupportRequest demande = getEntityOrThrow(id);
        verifierDroitsSurDemande(user, demande);
        return SupportRequestResponse.fromEntity(demande);
    }

    /** Verifie que l'utilisateur a le droit d'agir sur / consulter cette demande. */
    public void verifierDroitsSurDemande(User user, SupportRequest demande) {
        if (user.getRole() == Role.ADMIN) return;

        boolean estEleveProprietaire = user.getRole() == Role.ELEVE && demande.getEleve().getId().equals(user.getId());
        boolean estEnseignantAffecte = user.getRole() == Role.ENSEIGNANT
                && demande.getEnseignant() != null
                && demande.getEnseignant().getId().equals(user.getId());

        if (!estEleveProprietaire && !estEnseignantAffecte) {
            throw new BusinessRuleException("Vous n'avez pas acces a cette demande.");
        }
    }

    public SupportRequest getEntityOrThrow(Long id) {
        return supportRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demande introuvable avec id=" + id));
    }
}
