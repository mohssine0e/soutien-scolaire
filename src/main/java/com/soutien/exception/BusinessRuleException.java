package com.soutien.exception;

/**
 * Levee quand une regle metier est violee (ex: transition de statut invalide,
 * email deja utilise, acces a une ressource qui n'appartient pas a l'utilisateur, etc.)
 */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
