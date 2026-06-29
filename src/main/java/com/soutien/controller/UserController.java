package com.soutien.controller;

import com.soutien.dto.UserResponse;
import com.soutien.entity.Role;
import com.soutien.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Gestion des utilisateurs : reservee a l'administrateur.
 * (la creation de comptes se fait via /api/auth/register pour permettre l'auto-inscription)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> findAll(@RequestParam(required = false) Role role) {
        return role != null ? userService.findByRole(role) : userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
