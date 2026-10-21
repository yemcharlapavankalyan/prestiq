package org.prestiq.scholarship.service;

import org.prestiq.scholarship.domain.entity.PublicationStatus;
import org.prestiq.scholarship.domain.entity.SourceType;
import org.prestiq.scholarship.domain.entity.VerificationStatus;
import org.prestiq.scholarship.dto.request.ScholarshipArchiveRequest;
import org.prestiq.scholarship.dto.request.ScholarshipCreateRequest;
import org.prestiq.scholarship.dto.request.ScholarshipRejectRequest;
import org.prestiq.scholarship.dto.request.ScholarshipUpdateRequest;
import org.prestiq.scholarship.dto.response.PagedResponse;
import org.prestiq.scholarship.dto.response.ScholarshipResponse;
import org.prestiq.scholarship.dto.response.ScholarshipSummaryResponse;
import org.prestiq.scholarship.security.UserPrincipal;

import java.math.BigDecimal;

public interface ScholarshipService {

    // Public API
    PagedResponse<ScholarshipSummaryResponse> searchPublishedScholarships(
            String query,
            String educationLevel,
            String country,
            BigDecimal minAmount,
            BigDecimal maxAmount,
            SourceType sourceType,
            boolean includeExpired,
            int page,
            int size,
            String sort
    );

    ScholarshipResponse getPublishedScholarshipByIdOrSlug(String idOrSlug);

    // Administrative API
    ScholarshipResponse createDraft(ScholarshipCreateRequest request, UserPrincipal principal);

    ScholarshipResponse updateScholarship(Long id, ScholarshipUpdateRequest request);

    ScholarshipResponse submitForReview(Long id);

    ScholarshipResponse verifyScholarship(Long id, UserPrincipal principal);

    ScholarshipResponse rejectScholarship(Long id, ScholarshipRejectRequest request, UserPrincipal principal);

    ScholarshipResponse publishScholarship(Long id, UserPrincipal principal);

    ScholarshipResponse archiveScholarship(Long id, ScholarshipArchiveRequest request, UserPrincipal principal);

    ScholarshipResponse getScholarshipByIdAdmin(Long id);

    PagedResponse<ScholarshipSummaryResponse> searchAdminScholarships(
            String query,
            VerificationStatus verificationStatus,
            PublicationStatus publicationStatus,
            SourceType sourceType,
            int page,
            int size,
            String sort
    );
}
