package com.soutien.service;

import com.soutien.dto.SupportRequestCreateRequest;
import com.soutien.dto.SupportRequestResponse;
import com.soutien.entity.*;
import com.soutien.exception.BusinessRuleException;
import com.soutien.repository.SupportRequestRepository;
import com.soutien.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest {

    @Mock private SupportRequestRepository supportRequestRepository;
    @Mock private UserRepository userRepository;
    @Mock private SubjectService subjectService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private SupportRequestService supportRequestService;

    private User eleve;
    private User enseignant;
    private Subject subject;

    @BeforeEach
    void setUp() {
        eleve = User.builder().id(1L).nom("Eleve Test").email("eleve@test.ma").role(Role.ELEVE).build();
        enseignant = User.builder().id(2L).nom("Enseignant Test").email("ens@test.ma").role(Role.ENSEIGNANT).build();
        subject = Subject.builder().id(1L).nom("Mathematiques").build();
    }

    @Test
    void creerDemande_doitReussir_quandUtilisateurEstEleve() {
        when(securityUtils.getCurrentUser()).thenReturn(eleve);
        when(subjectService.getEntityOrThrow(1L)).thenReturn(subject);
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(inv -> {
            SupportRequest r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        SupportRequestResponse response = supportRequestService.creerDemande(
                new SupportRequestCreateRequest(1L, "Besoin d'aide en algebre"));

        assertEquals(RequestStatus.CREEE, response.status());
        assertEquals("Eleve Test", response.eleveNom());
        verify(supportRequestRepository).save(any(SupportRequest.class));
    }

    @Test
    void creerDemande_doitEchouer_quandUtilisateurNestPasEleve() {
        when(securityUtils.getCurrentUser()).thenReturn(enseignant);

        assertThrows(BusinessRuleException.class, () ->
                supportRequestService.creerDemande(new SupportRequestCreateRequest(1L, "test")));
    }

    @Test
    void affecterEnseignant_doitEchouer_siDemandeDejaAffectee() {
        SupportRequest demande = SupportRequest.builder()
                .id(5L).eleve(eleve).subject(subject).enseignant(enseignant)
                .status(RequestStatus.EN_COURS).build();

        when(securityUtils.getCurrentUser()).thenReturn(enseignant);
        when(supportRequestRepository.findById(5L)).thenReturn(Optional.of(demande));

        assertThrows(BusinessRuleException.class, () -> supportRequestService.affecterEnseignant(5L));
    }

    @Test
    void affecterEnseignant_doitReussir_siDemandeDisponible() {
        SupportRequest demande = SupportRequest.builder()
                .id(5L).eleve(eleve).subject(subject)
                .status(RequestStatus.CREEE).build();

        when(securityUtils.getCurrentUser()).thenReturn(enseignant);
        when(supportRequestRepository.findById(5L)).thenReturn(Optional.of(demande));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportRequestResponse response = supportRequestService.affecterEnseignant(5L);

        assertEquals(RequestStatus.EN_COURS, response.status());
        assertEquals(2L, response.enseignantId());
    }

    @Test
    void changerStatut_doitEchouer_pourTransitionInvalide() {
        SupportRequest demande = SupportRequest.builder()
                .id(5L).eleve(eleve).subject(subject)
                .status(RequestStatus.TERMINEE).build();

        when(securityUtils.getCurrentUser()).thenReturn(eleve);
        when(supportRequestRepository.findById(5L)).thenReturn(Optional.of(demande));

        assertThrows(BusinessRuleException.class, () ->
                supportRequestService.changerStatut(5L, RequestStatus.EN_COURS));
    }

    @Test
    void changerStatut_doitReussir_pourTransitionValide() {
        SupportRequest demande = SupportRequest.builder()
                .id(5L).eleve(eleve).subject(subject)
                .status(RequestStatus.CREEE).build();

        when(securityUtils.getCurrentUser()).thenReturn(eleve);
        when(supportRequestRepository.findById(5L)).thenReturn(Optional.of(demande));
        when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        SupportRequestResponse response = supportRequestService.changerStatut(5L, RequestStatus.ANNULEE);

        assertEquals(RequestStatus.ANNULEE, response.status());
    }

    @Test
    void verifierDroitsSurDemande_doitEchouer_pourUnTiers() {
        User intrus = User.builder().id(99L).nom("Intrus").role(Role.ELEVE).build();
        SupportRequest demande = SupportRequest.builder()
                .id(5L).eleve(eleve).subject(subject).status(RequestStatus.CREEE).build();

        assertThrows(BusinessRuleException.class, () ->
                supportRequestService.verifierDroitsSurDemande(intrus, demande));
    }
}
