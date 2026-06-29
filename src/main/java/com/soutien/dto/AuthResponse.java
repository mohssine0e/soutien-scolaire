package com.soutien.dto;

import com.soutien.entity.Role;

public record AuthResponse(
        String token,
        Long userId,
        String nom,
        String email,
        Role role
) {}
