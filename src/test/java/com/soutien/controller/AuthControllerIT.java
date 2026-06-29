package com.soutien.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soutien.dto.LoginRequest;
import com.soutien.dto.RegisterRequest;
import com.soutien.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test d'integration : verifie le flux complet inscription -> connexion
 * avec une vraie base H2 en memoire (contexte Spring complet).
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void inscriptionPuisConnexion_doiventReussir() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Jean Test", "jean.test@example.com", "motdepasse123", Role.ELEVE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ELEVE"));

        LoginRequest loginRequest = new LoginRequest("jean.test@example.com", "motdepasse123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    void inscription_doitEchouer_siEmailDejaUtilise() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Doublon", "doublon@example.com", "motdepasse123", Role.ELEVE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void inscription_doitEchouer_siEmailInvalide() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "Invalide", "pas-un-email", "motdepasse123", Role.ELEVE);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest());
    }
}
