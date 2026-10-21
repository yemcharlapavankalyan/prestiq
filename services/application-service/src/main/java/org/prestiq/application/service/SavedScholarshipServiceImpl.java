package org.prestiq.application.service;

import org.prestiq.application.client.ScholarshipServiceClient;
import org.prestiq.application.domain.entity.SavedScholarship;
import org.prestiq.application.dto.client.ScholarshipClientDto;
import org.prestiq.application.dto.request.SaveScholarshipRequest;
import org.prestiq.application.dto.response.PagedResponse;
import org.prestiq.application.dto.response.SavedScholarshipResponse;
import org.prestiq.application.exception.SavedScholarshipNotFoundException;
import org.prestiq.application.repository.SavedScholarshipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class SavedScholarshipServiceImpl implements SavedScholarshipService {

    private static final Logger log = LoggerFactory.getLogger(SavedScholarshipServiceImpl.class);

    private final SavedScholarshipRepository savedScholarshipRepository;
    private final ScholarshipServiceClient scholarshipServiceClient;

    public SavedScholarshipServiceImpl(
            SavedScholarshipRepository savedScholarshipRepository,
            ScholarshipServiceClient scholarshipServiceClient
    ) {
        this.savedScholarshipRepository = savedScholarshipRepository;
        this.scholarshipServiceClient = scholarshipServiceClient;
    }

    @Override
    @Transactional
    public SavedScholarshipResponse saveScholarship(
            UUID studentId,
            Long scholarshipId,
            SaveScholarshipRequest request
    ) {
        Optional<SavedScholarship> existingOpt = savedScholarshipRepository
                .findByStudentIdAndScholarshipId(studentId, scholarshipId);

        if (existingOpt.isPresent()) {
            SavedScholarship existing = existingOpt.get();
            if (request != null && request.notes() != null) {
                existing.setNotes(request.notes());
                savedScholarshipRepository.save(existing);
            }
            return SavedScholarshipResponse.fromEntity(existing);
        }

        String title = (request != null) ? request.scholarshipTitle() : null;
        String provider = (request != null) ? request.scholarshipProvider() : null;
        LocalDate deadline = (request != null) ? request.applicationDeadline() : null;
        BigDecimal awardMax = (request != null) ? request.awardAmountMax() : null;
        String notes = (request != null) ? request.notes() : null;

        // Fetch verified metadata from scholarship-service if available
        Optional<ScholarshipClientDto> scholOpt = scholarshipServiceClient.fetchScholarshipById(scholarshipId);
        if (scholOpt.isPresent()) {
            ScholarshipClientDto dto = scholOpt.get();
            if (!StringUtils.hasText(title)) title = dto.title();
            if (!StringUtils.hasText(provider)) provider = dto.providerName();
            if (deadline == null) deadline = dto.applicationDeadline();
            if (awardMax == null) awardMax = dto.awardAmountMax();
        }

        if (!StringUtils.hasText(title)) {
            title = "Scholarship #" + scholarshipId;
        }

        SavedScholarship saved = new SavedScholarship(
                studentId,
                scholarshipId,
                title,
                provider,
                deadline,
                awardMax,
                notes
        );

        SavedScholarship persisted = savedScholarshipRepository.save(saved);
        log.info("Saved scholarship {} for student {}", scholarshipId, studentId);
        return SavedScholarshipResponse.fromEntity(persisted);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<SavedScholarshipResponse> getSavedScholarships(
            UUID studentId,
            Pageable pageable
    ) {
        Page<SavedScholarship> page = savedScholarshipRepository.findByStudentId(studentId, pageable);
        return PagedResponse.fromPage(page.map(SavedScholarshipResponse::fromEntity));
    }

    @Override
    @Transactional
    public void removeSavedScholarship(UUID studentId, Long scholarshipId) {
        if (!savedScholarshipRepository.existsByStudentIdAndScholarshipId(studentId, scholarshipId)) {
            throw new SavedScholarshipNotFoundException(
                    "Saved scholarship bookmark not found for scholarship ID: " + scholarshipId);
        }
        savedScholarshipRepository.deleteByStudentIdAndScholarshipId(studentId, scholarshipId);
        log.info("Removed saved scholarship {} for student {}", scholarshipId, studentId);
    }
}
