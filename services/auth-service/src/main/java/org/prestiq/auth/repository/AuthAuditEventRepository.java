package org.prestiq.auth.repository;

import org.prestiq.auth.domain.entity.AuthAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuthAuditEventRepository extends JpaRepository<AuthAuditEvent, Long> {

    List<AuthAuditEvent> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<AuthAuditEvent> findByEmailOrderByCreatedAtDesc(String email);
}
