package org.prestiq.auth.service;

import org.prestiq.auth.domain.entity.AuthAuditEvent;
import org.prestiq.auth.repository.AuthAuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuthAuditEventRepository auditEventRepository;

    public AuditLogService(AuthAuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logEvent(UUID userId, String email, String eventType, String ipAddress, String userAgent, String details) {
        try {
            AuthAuditEvent event = new AuthAuditEvent(userId, email, eventType, ipAddress, userAgent, details);
            auditEventRepository.save(event);
            log.info("Auth Audit Event: type='{}', email='{}', ip='{}'", eventType, maskEmail(email), ipAddress);
        } catch (Exception e) {
            log.error("Failed to persist authentication audit event: ", e);
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "[ANONYMOUS]";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "*@" + email.substring(atIndex + 1);
        }
        return email.charAt(0) + "***" + email.charAt(atIndex - 1) + "@" + email.substring(atIndex + 1);
    }
}
