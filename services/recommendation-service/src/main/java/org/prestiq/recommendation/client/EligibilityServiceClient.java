package org.prestiq.recommendation.client;

import org.prestiq.recommendation.config.JwtProperties;
import org.prestiq.recommendation.dto.client.BatchEvaluateItemDto;
import org.prestiq.recommendation.dto.client.BatchEvaluateRequestDto;
import org.prestiq.recommendation.dto.client.BatchEvaluateResponseDto;
import org.prestiq.recommendation.dto.client.StudentProfileClientDto;
import org.prestiq.recommendation.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class EligibilityServiceClient {

    private static final Logger log = LoggerFactory.getLogger(EligibilityServiceClient.class);

    private final RestClient restClient;
    private final String eligibilityServiceUrl;
    private final String internalServiceKey;

    public EligibilityServiceClient(
            RestClient restClient,
            @Value("${prestiq.services.eligibility-service.url:http://localhost:8084}") String eligibilityServiceUrl,
            JwtProperties jwtProperties
    ) {
        this.restClient = restClient;
        this.eligibilityServiceUrl = eligibilityServiceUrl;
        this.internalServiceKey = jwtProperties.getInternalServiceKey();
    }

    public Map<Long, BatchEvaluateItemDto> batchEvaluate(
            StudentProfileClientDto profile,
            List<Long> scholarshipIds
    ) {
        if (scholarshipIds == null || scholarshipIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String url = eligibilityServiceUrl + "/internal/v1/eligibility/batch-evaluate";
        BatchEvaluateRequestDto request = new BatchEvaluateRequestDto(profile, scholarshipIds);

        try {
            BatchEvaluateResponseDto response = restClient.post()
                    .uri(url)
                    .header("X-Internal-Service-Key", internalServiceKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        log.error("Failed to batch evaluate with eligibility-service. Status: {}", resp.getStatusCode());
                        throw new DownstreamServiceException("eligibility-service", resp.getStatusCode().value(),
                                "Error communicating with Eligibility Service: HTTP " + resp.getStatusCode());
                    })
                    .body(BatchEvaluateResponseDto.class);

            if (response == null || response.evaluations() == null) {
                return Collections.emptyMap();
            }

            return response.evaluations().stream()
                    .collect(Collectors.toMap(BatchEvaluateItemDto::scholarshipId, Function.identity(), (e1, e2) -> e1));
        } catch (DownstreamServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Exception communicating with Eligibility Service at {}: {}", url, e.getMessage());
            throw new DownstreamServiceException("eligibility-service", 503,
                    "Eligibility Service unavailable: " + e.getMessage());
        }
    }
}
