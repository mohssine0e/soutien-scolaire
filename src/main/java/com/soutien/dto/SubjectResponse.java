package com.soutien.dto;

import com.soutien.entity.Subject;

public record SubjectResponse(Long id, String nom, String description) {
    public static SubjectResponse fromEntity(Subject subject) {
        return new SubjectResponse(subject.getId(), subject.getNom(), subject.getDescription());
    }
}
