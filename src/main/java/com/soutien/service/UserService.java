package com.soutien.service;

import com.soutien.dto.UserResponse;
import com.soutien.entity.Role;
import com.soutien.exception.ResourceNotFoundException;
import com.soutien.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserResponse::fromEntity).toList();
    }

    public List<UserResponse> findByRole(Role role) {
        return userRepository.findByRole(role).stream().map(UserResponse::fromEntity).toList();
    }

    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec id=" + id));
    }

    public void delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur introuvable avec id=" + id);
        }
        userRepository.deleteById(id);
    }
}
