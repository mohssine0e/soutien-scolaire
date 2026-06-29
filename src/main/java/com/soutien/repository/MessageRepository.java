package com.soutien.repository;

import com.soutien.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySupportRequestIdOrderByDateEnvoiAsc(Long supportRequestId);
}
