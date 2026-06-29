package com.soutien.dto;

import jakarta.validation.constraints.NotNull;

public record SupportRequestCreateRequest(
        @NotNull(message = "L'identifiant de la matiere est obligatoire")
        Long subjectId,

        String description
) {}
