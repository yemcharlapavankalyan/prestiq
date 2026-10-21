package org.prestiq.scholarship.service;

import org.prestiq.scholarship.domain.entity.Scholarship;
import org.prestiq.scholarship.domain.entity.ScholarshipSource;
import org.prestiq.scholarship.dto.request.ScholarshipSourceRequest;
import org.prestiq.scholarship.dto.response.ScholarshipSourceResponse;
import org.prestiq.scholarship.exception.ScholarshipNotFoundException;
import org.prestiq.scholarship.repository.ScholarshipRepository;
import org.prestiq.scholarship.repository.ScholarshipSourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class ScholarshipSourceServiceImpl implements ScholarshipSourceService {

    private static final Logger log = LoggerFactory.getLogger(ScholarshipSourceServiceImpl.class);

    private final ScholarshipRepository scholarshipRepository;
    private final ScholarshipSourceRepository sourceRepository;

    public ScholarshipSourceServiceImpl(ScholarshipRepository scholarshipRepository, ScholarshipSourceRepository sourceRepository) {
        this.scholarshipRepository = scholarshipRepository;
        this.sourceRepository = sourceRepository;
    }

    @Override
    public ScholarshipSourceResponse addSource(Long scholarshipId, ScholarshipSourceRequest request) {
        Scholarship scholarship = scholarshipRepository.findById(scholarshipId)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + scholarshipId));

        ScholarshipSource source = new ScholarshipSource(
                scholarship,
                request.sourceUrl(),
                request.sourceName(),
                request.contentHash(),
                Instant.now(),
                request.httpStatus() != null ? request.httpStatus() : 200
        );

        ScholarshipSource saved = sourceRepository.save(source);
        log.info("Added provenance source {} to scholarship {}", saved.getId(), scholarshipId);
        return ScholarshipSourceResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScholarshipSourceResponse> getSourcesByScholarshipId(Long scholarshipId) {
        if (!scholarshipRepository.existsById(scholarshipId)) {
            throw new ScholarshipNotFoundException("Scholarship not found with ID: " + scholarshipId);
        }
        return sourceRepository.findByScholarshipId(scholarshipId).stream()
                .map(ScholarshipSourceResponse::fromEntity)
                .toList();
    }

    @Override
    public void removeSource(Long scholarshipId, Long sourceId) {
        Scholarship scholarship = scholarshipRepository.findById(scholarshipId)
                .orElseThrow(() -> new ScholarshipNotFoundException("Scholarship not found with ID: " + scholarshipId));

        ScholarshipSource source = sourceRepository.findById(sourceId)
                .orElseThrow(() -> new ScholarshipNotFoundException("Source not found with ID: " + sourceId));

        if (!source.getScholarship().getId().equals(scholarship.getId())) {
            throw new IllegalArgumentException("Source " + sourceId + " does not belong to scholarship " + scholarshipId);
        }

        sourceRepository.delete(source);
        log.info("Removed provenance source {} from scholarship {}", sourceId, scholarshipId);
    }
}
