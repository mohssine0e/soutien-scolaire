package com.soutien.repository;

import com.soutien.entity.RequestStatus;
import com.soutien.entity.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    List<SupportRequest> findByStatus(RequestStatus status);

    List<SupportRequest> findByEleveId(Long eleveId);

    List<SupportRequest> findByEnseignantId(Long enseignantId);

    // Demandes disponibles pour un enseignant = pas encore affectees
    List<SupportRequest> findByStatusAndEnseignantIsNull(RequestStatus status);
}
