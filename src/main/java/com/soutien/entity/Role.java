package com.soutien.entity;

/**
 * Roles disponibles sur la plateforme.
 * Utilise par Spring Security pour l'autorisation (prefixe ROLE_ ajoute automatiquement).
 */
public enum Role {
    ELEVE,
    ENSEIGNANT,
    ADMIN
}
