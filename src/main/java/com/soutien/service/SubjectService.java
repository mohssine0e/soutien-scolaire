package com.soutien.service;

import com.soutien.dto.SubjectRequest;
import com.soutien.dto.SubjectResponse;
import com.soutien.entity.Subject;
import com.soutien.exception.BusinessRuleException;
import com.soutien.exception.ResourceNotFoundException;
import com.soutien.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public List<SubjectResponse> findAll() {
        return subjectRepository.findAll().stream().map(SubjectResponse::fromEntity).toList();
    }

    public SubjectResponse findById(Long id) {
        return subjectRepository.findById(id)
                .map(SubjectResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Matiere introuvable avec id=" + id));
    }

    public SubjectResponse create(SubjectRequest request) {
        if (subjectRepository.findByNomIgnoreCase(request.nom()).isPresent()) {
            throw new BusinessRuleException("Cette matiere existe deja.");
        }
        Subject subject = Subject.builder()
                .nom(request.nom())
                .description(request.description())
                .build();
        return SubjectResponse.fromEntity(subjectRepository.save(subject));
    }

    public SubjectResponse update(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matiere introuvable avec id=" + id));
        subject.setNom(request.nom());
        subject.setDescription(request.description());
        return SubjectResponse.fromEntity(subjectRepository.save(subject));
    }

    public void delete(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("Matiere introuvable avec id=" + id);
        }
        subjectRepository.deleteById(id);
    }

    // Utilise par d'autres services pour resoudre l'entite Subject
    public Subject getEntityOrThrow(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Matiere introuvable avec id=" + id));
    }
}
