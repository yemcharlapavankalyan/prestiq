package org.prestiq.recommendation.client;

import org.prestiq.recommendation.dto.client.ScholarshipPageClientDto;
import org.prestiq.recommendation.dto.client.ScholarshipSummaryClientDto;
import org.prestiq.recommendation.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

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

    public List<ScholarshipSummaryClientDto> fetchActivePublishedScholarships(int limit) {
        String url = scholarshipServiceUrl + "/api/v1/scholarships?size=" + limit + "&includeExpired=false";
        try {
            ScholarshipPageClientDto page = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.error("Failed to fetch scholarships from scholarship-service. Status: {}", resp.getStatusCode());
                        throw new DownstreamServiceException("scholarship-service", resp.getStatusCode().value(),
                                "Error communicating with Scholarship Service: HTTP " + resp.getStatusCode());
                    })
                    .body(ScholarshipPageClientDto.class);

            return (page != null && page.content() != null) ? page.content() : Collections.emptyList();
        } catch (DownstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception communicating with Scholarship Service at {}: {}", url, e.getMessage());
            throw new DownstreamServiceException("scholarship-service", 503,
                    "Scholarship Service unavailable: " + e.getMessage());
        }
    }

    public java.util.Optional<ScholarshipSummaryClientDto> fetchScholarshipById(Long id) {
        String url = scholarshipServiceUrl + "/api/v1/scholarships/" + id;
        try {
            ScholarshipSummaryClientDto dto = restClient.get()
                    .uri(url)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        // 404 Not found
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.error("Failed to fetch scholarship {}. Status: {}", id, resp.getStatusCode());
                        throw new DownstreamServiceException("scholarship-service", resp.getStatusCode().value(),
                                "Error communicating with Scholarship Service: HTTP " + resp.getStatusCode());
                    })
                    .body(ScholarshipSummaryClientDto.class);
            return java.util.Optional.ofNullable(dto);
        } catch (DownstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Could not fetch scholarship details for id {}: {}", id, e.getMessage());
            return java.util.Optional.empty();
        }
    }
}
