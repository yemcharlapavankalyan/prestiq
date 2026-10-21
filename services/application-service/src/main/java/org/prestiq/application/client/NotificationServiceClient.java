package org.prestiq.application.client;

import org.prestiq.application.config.JwtProperties;
import org.prestiq.application.dto.client.DeadlineReminderRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class NotificationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceClient.class);

    private final RestClient restClient;
    private final String notificationServiceUrl;
    private final String internalServiceKey;

    public NotificationServiceClient(
            RestClient restClient,
            @Value("${prestiq.services.notification-service.url:http://localhost:8087}") String notificationServiceUrl,
            JwtProperties jwtProperties
    ) {
        this.restClient = restClient;
        this.notificationServiceUrl = notificationServiceUrl;
        this.internalServiceKey = jwtProperties.getInternalServiceKey();
    }

    public void scheduleDeadlineReminder(UUID applicationId, UUID studentId, LocalDate deadline) {
        if (deadline == null) return;
        String url = notificationServiceUrl + "/internal/v1/notifications/deadlines";
        try {
            DeadlineReminderRequestDto payload = new DeadlineReminderRequestDto(applicationId, studentId, deadline);
            restClient.post()
                    .uri(url)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully scheduled deadline reminders for application {}", applicationId);
        } catch (Exception e) {
            log.warn("Could not dispatch deadline reminder to notification-service at {}: {}. Application saved successfully.",
                    url, e.getMessage());
        }
    }
}
