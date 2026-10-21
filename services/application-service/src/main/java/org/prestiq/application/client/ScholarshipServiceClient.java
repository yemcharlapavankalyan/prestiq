package org.prestiq.application.client;

import org.prestiq.application.dto.client.ScholarshipClientDto;
import org.prestiq.application.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class ScholarshipServiceClient {

    private static final Logger log = LoggerFactory.getLogger(ScholarshipServiceClient.class);

    private final RestClient restClient;
    private final String scholarshipServiceUrl;

    public ScholarshipServiceClient(
            RestClient restClient,
            @Value("${prestiq.services.scholarship-service.url:http://localhost:8083}") String scholarshipServiceUrl
    ) {
        this.restClient = restClient;
        this.scholarshipServiceUrl = scholarshipServiceUrl;
    }

    public Optional<ScholarshipClientDto> fetchScholarshipById(Long id) {
        String url = scholarshipServiceUrl + "/api/v1/scholarships/" + id;
        try {
            ScholarshipClientDto dto = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        // Scholarship not found
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.error("Failed to fetch scholarship {}. Status: {}", id, resp.getStatusCode());
                        throw new DownstreamServiceException("scholarship-service", resp.getStatusCode().value(),
                                "Error communicating with Scholarship Service: HTTP " + resp.getStatusCode());
                    })
                    .body(ScholarshipClientDto.class);

            return Optional.ofNullable(dto);
        } catch (DownstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not retrieve scholarship details for ID {} from {}: {}", id, url, e.getMessage());
            return Optional.empty();
        }
    }
}
