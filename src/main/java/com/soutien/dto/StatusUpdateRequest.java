package com.soutien.dto;

import com.soutien.entity.RequestStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Le nouveau statut est obligatoire")
        RequestStatus status
) {}
