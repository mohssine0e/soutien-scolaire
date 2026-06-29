package com.soutien.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageCreateRequest(
        @NotBlank(message = "Le contenu du message est obligatoire")
        String contenu
) {}
