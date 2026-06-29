package com.soutien.service;

import com.soutien.dto.MessageCreateRequest;
import com.soutien.dto.MessageResponse;
import com.soutien.entity.Message;
import com.soutien.entity.SupportRequest;
import com.soutien.entity.User;
import com.soutien.exception.BusinessRuleException;
import com.soutien.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final SupportRequestService supportRequestService;
    private final SecurityUtils securityUtils;

    /** Envoie un message lie a une demande. Seuls l'eleve et l'enseignant affectes peuvent ecrire. */
    public MessageResponse envoyerMessage(Long requestId, MessageCreateRequest request) {
        User auteur = securityUtils.getCurrentUser();
        SupportRequest demande = supportRequestService.getEntityOrThrow(requestId);

        supportRequestService.verifierDroitsSurDemande(auteur, demande);

        if (demande.getEnseignant() == null) {
            throw new BusinessRuleException("La messagerie n'est disponible qu'apres affectation d'un enseignant a la demande.");
        }

        Message message = Message.builder()
                .supportRequest(demande)
                .auteur(auteur)
                .contenu(request.contenu())
                .build();

        return MessageResponse.fromEntity(messageRepository.save(message));
    }

    /** Historique des messages d'une demande, accessible a l'eleve, l'enseignant affecte ou l'admin. */
    public List<MessageResponse> historique(Long requestId) {
        User user = securityUtils.getCurrentUser();
        SupportRequest demande = supportRequestService.getEntityOrThrow(requestId);

        supportRequestService.verifierDroitsSurDemande(user, demande);

        return messageRepository.findBySupportRequestIdOrderByDateEnvoiAsc(requestId)
                .stream().map(MessageResponse::fromEntity).toList();
    }
}
