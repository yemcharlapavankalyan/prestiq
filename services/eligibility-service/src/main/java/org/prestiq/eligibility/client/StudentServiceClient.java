package org.prestiq.eligibility.client;

import org.prestiq.eligibility.config.JwtProperties;
import org.prestiq.eligibility.dto.response.StudentProfileDto;
import org.prestiq.eligibility.exception.StudentProfileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Component
public class StudentServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StudentServiceClient.class);

    private final RestClient restClient;
    private final String studentServiceUrl;
    private final String internalServiceKey;

    public StudentServiceClient(
            RestClient restClient,
            @Value("${prestiq.services.student-service.url:http://localhost:8082}") String studentServiceUrl,
            JwtProperties jwtProperties
    ) {
        this.restClient = restClient;
        this.studentServiceUrl = studentServiceUrl;
        this.internalServiceKey = jwtProperties.getInternalServiceKey();
    }

    public StudentProfileDto fetchStudentProfile(UUID userId) {
        String url = studentServiceUrl + "/internal/v1/students/" + userId + "/profile";
        try {
            return restClient.get()
                    .uri(url)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, resp) -> {
                        throw new StudentProfileNotFoundException("Student profile not found for user ID: " + userId);
                    })
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.error("Failed to fetch student profile from student-service. Status: {}", resp.getStatusCode());
                        throw new RuntimeException("Error communicating with Student Service: HTTP " + resp.getStatusCode());
                    })
                    .body(StudentProfileDto.class);
        } catch (StudentProfileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception communicating with Student Service at {}: {}", url, e.getMessage());
            throw new StudentProfileNotFoundException("Student profile could not be retrieved from student-service: " + e.getMessage());
        }
    }
}
