package org.prestiq.notification.client;

import org.prestiq.notification.client.dto.ApplicationClientDto;
import org.prestiq.notification.config.JwtProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.UUID;

@Component
public class ApplicationServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceClient.class);

    private final RestClient restClient;
    private final String applicationServiceUrl;
    private final String internalServiceKey;

    public ApplicationServiceClient(
            RestClient restClient,
            @Value("${prestiq.services.application-service.url:http://localhost:8086}") String applicationServiceUrl,
            JwtProperties jwtProperties
    ) {
        this.restClient = restClient;
        this.applicationServiceUrl = applicationServiceUrl;
        this.internalServiceKey = jwtProperties.getInternalServiceKey();
    }

    public Optional<ApplicationClientDto> getApplicationById(UUID applicationId) {
        if (applicationId == null) return Optional.empty();
        String url = applicationServiceUrl + "/internal/v1/applications/" + applicationId;

        try {
            ApplicationClientDto dto = restClient.get()
                    .uri(url)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(ApplicationClientDto.class);

            return Optional.ofNullable(dto);
        } catch (Exception e) {
            log.warn("Could not retrieve application {} from application-service at {}: {}. Proceeding with default text.",
                    applicationId, url, e.getMessage());
            return Optional.empty();
        }
    }
}
