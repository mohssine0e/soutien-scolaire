package com.soutien.dto;

import jakarta.validation.constraints.NotBlank;

public record SubjectRequest(
        @NotBlank(message = "Le nom de la matiere est obligatoire")
        String nom,
        String description
) {}
