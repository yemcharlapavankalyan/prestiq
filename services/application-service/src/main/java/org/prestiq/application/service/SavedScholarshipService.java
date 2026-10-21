package org.prestiq.application.service;

import org.prestiq.application.dto.request.SaveScholarshipRequest;
import org.prestiq.application.dto.response.PagedResponse;
import org.prestiq.application.dto.response.SavedScholarshipResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SavedScholarshipService {

    SavedScholarshipResponse saveScholarship(
            UUID studentId,
            Long scholarshipId,
            SaveScholarshipRequest request
    );

    PagedResponse<SavedScholarshipResponse> getSavedScholarships(
            UUID studentId,
            Pageable pageable
    );

    void removeSavedScholarship(UUID studentId, Long scholarshipId);
}
