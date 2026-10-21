package org.prestiq.application.service;

import org.prestiq.application.domain.entity.ApplicationStatus;
import org.prestiq.application.dto.request.CreateApplicationRequest;
import org.prestiq.application.dto.request.UpdateApplicationRequest;
import org.prestiq.application.dto.request.UpdateStatusRequest;
import org.prestiq.application.dto.response.ApplicationResponse;
import org.prestiq.application.dto.response.PagedResponse;
import org.prestiq.application.dto.response.StatusHistoryResponse;
import org.prestiq.application.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse createApplication(UUID studentId, CreateApplicationRequest request);

    PagedResponse<ApplicationResponse> getApplications(
            UUID studentId,
            ApplicationStatus status,
            Pageable pageable
    );

    ApplicationResponse getApplicationById(UUID id, UserPrincipal principal);

    ApplicationResponse updateApplication(
            UUID id,
            UserPrincipal principal,
            UpdateApplicationRequest request
    );

    ApplicationResponse updateApplicationStatus(
            UUID id,
            UserPrincipal principal,
            UpdateStatusRequest request
    );

    ApplicationResponse withdrawApplication(
            UUID id,
            UserPrincipal principal,
            String notes
    );

    List<StatusHistoryResponse> getStatusHistory(UUID id, UserPrincipal principal);

    List<ApplicationResponse> getUpcomingDeadlines(UUID studentId, int daysAhead);
}
