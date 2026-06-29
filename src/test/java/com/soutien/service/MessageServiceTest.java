package com.soutien.service;

import com.soutien.dto.MessageCreateRequest;
import com.soutien.dto.MessageResponse;
import com.soutien.entity.*;
import com.soutien.exception.BusinessRuleException;
import com.soutien.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private SupportRequestService supportRequestService;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private MessageService messageService;

    private User eleve;
    private User enseignant;
    private SupportRequest demandeAffectee;
    private SupportRequest demandeNonAffectee;

    @BeforeEach
    void setUp() {
        eleve = User.builder().id(1L).nom("Eleve").role(Role.ELEVE).build();
        enseignant = User.builder().id(2L).nom("Enseignant").role(Role.ENSEIGNANT).build();
        Subject subject = Subject.builder().id(1L).nom("Maths").build();

        demandeAffectee = SupportRequest.builder()
                .id(5L).eleve(eleve).enseignant(enseignant).subject(subject)
                .status(RequestStatus.EN_COURS).build();

        demandeNonAffectee = SupportRequest.builder()
                .id(6L).eleve(eleve).subject(subject)
                .status(RequestStatus.CREEE).build();
    }

    @Test
    void envoyerMessage_doitReussir_siEnseignantAffecte() {
        when(securityUtils.getCurrentUser()).thenReturn(eleve);
        when(supportRequestService.getEntityOrThrow(5L)).thenReturn(demandeAffectee);
        when(messageRepository.save(any(Message.class))).thenAnswer(inv -> {
            Message m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });

        MessageResponse response = messageService.envoyerMessage(5L, new MessageCreateRequest("Bonjour, j'ai une question."));

        assertEquals("Bonjour, j'ai une question.", response.contenu());
        assertEquals(1L, response.auteurId());
    }

    @Test
    void envoyerMessage_doitEchouer_siAucunEnseignantAffecte() {
        when(securityUtils.getCurrentUser()).thenReturn(eleve);
        when(supportRequestService.getEntityOrThrow(6L)).thenReturn(demandeNonAffectee);

        assertThrows(BusinessRuleException.class, () ->
                messageService.envoyerMessage(6L, new MessageCreateRequest("test")));
    }

    @Test
    void envoyerMessage_doitEchouer_siUtilisateurNonAutorise() {
        User intrus = User.builder().id(99L).nom("Intrus").role(Role.ELEVE).build();
        when(securityUtils.getCurrentUser()).thenReturn(intrus);
        when(supportRequestService.getEntityOrThrow(5L)).thenReturn(demandeAffectee);
        doThrow(new BusinessRuleException("Vous n'avez pas acces a cette demande."))
                .when(supportRequestService).verifierDroitsSurDemande(intrus, demandeAffectee);

        assertThrows(BusinessRuleException.class, () ->
                messageService.envoyerMessage(5L, new MessageCreateRequest("test")));
    }
}
