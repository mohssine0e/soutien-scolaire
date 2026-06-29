package com.soutien.dto;

import com.soutien.entity.Role;
import com.soutien.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String nom,
        String email,
        Role role,
        LocalDateTime dateCreation
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(user.getId(), user.getNom(), user.getEmail(), user.getRole(), user.getDateCreation());
    }
}
