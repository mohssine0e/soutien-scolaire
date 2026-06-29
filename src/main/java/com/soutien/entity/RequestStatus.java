package com.soutien.entity;

/**
 * Statuts possibles d'une demande de soutien scolaire.
 * Transitions autorisees (gerees dans SupportRequestService) :
 * CREEE -> EN_COURS -> TERMINEE
 * CREEE -> ANNULEE
 * EN_COURS -> ANNULEE
 */
public enum RequestStatus {
    CREEE,
    EN_COURS,
    TERMINEE,
    ANNULEE
}
