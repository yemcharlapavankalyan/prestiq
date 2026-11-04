package org.prestiq.application.service;

import org.prestiq.application.client.ScholarshipServiceClient;
import org.prestiq.application.domain.entity.ApplicationStatus;
import org.prestiq.application.domain.entity.ApplicationStatusHistory;
import org.prestiq.application.domain.entity.ScholarshipApplication;
import org.prestiq.application.dto.client.ScholarshipClientDto;
import org.prestiq.application.dto.request.CreateApplicationRequest;
import org.prestiq.application.dto.request.UpdateApplicationRequest;
import org.prestiq.application.dto.request.UpdateStatusRequest;
import org.prestiq.application.dto.response.ApplicationResponse;
import org.prestiq.application.dto.response.PagedResponse;
import org.prestiq.application.dto.response.StatusHistoryResponse;
import org.prestiq.application.exception.ApplicationNotFoundException;
import org.prestiq.application.exception.DuplicateApplicationException;
import org.prestiq.application.exception.InvalidStatusTransitionException;
import org.prestiq.application.repository.ApplicationStatusHistoryRepository;
import org.prestiq.application.repository.ScholarshipApplicationRepository;
import org.prestiq.application.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private final ScholarshipApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ScholarshipServiceClient scholarshipClient;

    public ApplicationServiceImpl(
            ScholarshipApplicationRepository applicationRepository,
            ApplicationStatusHistoryRepository historyRepository,
            ScholarshipServiceClient scholarshipClient
    ) {
        this.applicationRepository = applicationRepository;
        this.historyRepository = historyRepository;
        this.scholarshipClient = scholarshipClient;
    }

    @Override
    public ApplicationResponse createApplication(UUID studentId, CreateApplicationRequest request) {
        Set<ApplicationStatus> terminalStatuses = Set.of(ApplicationStatus.WITHDRAWN);
        boolean exists = applicationRepository.existsByStudentIdAndScholarshipIdAndCurrentStatusNotIn(
                studentId, request.scholarshipId(), terminalStatuses
        );
        if (exists) {
            throw new DuplicateApplicationException("Active application already exists for scholarship ID: " + request.scholarshipId());
        }

        String title = request.scholarshipTitle();
        String provider = request.scholarshipProvider();
        LocalDate deadline = request.submissionDeadline();

        Optional<ScholarshipClientDto> clientDto = scholarshipClient.fetchScholarshipById(request.scholarshipId());
        if (clientDto.isPresent()) {
            ScholarshipClientDto dto = clientDto.get();
            if (dto.title() != null) title = dto.title();
            if (dto.providerName() != null) provider = dto.providerName();
            if (dto.applicationDeadline() != null) deadline = dto.applicationDeadline();
        }

        if (title == null || title.isBlank()) {
            title = "Scholarship #" + request.scholarshipId();
        }
        if (deadline == null) {
            deadline = LocalDate.now().plusMonths(1);
        }

        ScholarshipApplication app = new ScholarshipApplication();
        app.setId(UUID.randomUUID());
        app.setStudentId(studentId);
        app.setScholarshipId(request.scholarshipId());
        app.setScholarshipTitle(title);
        app.setScholarshipProvider(provider);
        app.setCurrentStatus(ApplicationStatus.PLANNING);
        app.setSubmissionDeadline(deadline);
        app.setExternalPortalReference(request.externalPortalReference());
        app.setNotes(request.notes());
        app.setProgressPercentage(0);
        app.setCreatedAt(Instant.now());
        app.setUpdatedAt(Instant.now());

        ScholarshipApplication saved = applicationRepository.save(app);

        // Record history
        ApplicationStatusHistory history = new ApplicationStatusHistory(
                saved.getId(), null, ApplicationStatus.PLANNING, studentId, "Initial application created"
        );
        historyRepository.save(history);

        log.info("Created application {} for student {} scholarship {}", saved.getId(), studentId, request.scholarshipId());
        return ApplicationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getApplications(UUID studentId, ApplicationStatus status, Pageable pageable) {
        Page<ScholarshipApplication> page;
        if (status != null) {
            page = applicationRepository.findByStudentIdAndCurrentStatus(studentId, status, pageable);
        } else {
            page = applicationRepository.findByStudentId(studentId, pageable);
        }
        return PagedResponse.fromPage(page.map(ApplicationResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UUID id, UserPrincipal principal) {
        ScholarshipApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + id));

        checkAccess(app, principal);
        return ApplicationResponse.fromEntity(app);
    }

    @Override
    public ApplicationResponse updateApplication(UUID id, UserPrincipal principal, UpdateApplicationRequest request) {
        ScholarshipApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + id));

        checkAccess(app, principal);

        if (request.progressPercentage() != null) {
            app.setProgressPercentage(request.progressPercentage());
        }
        if (request.notes() != null) {
            app.setNotes(request.notes());
        }
        if (request.externalPortalReference() != null) {
            app.setExternalPortalReference(request.externalPortalReference());
        }
        if (request.submissionDeadline() != null) {
            app.setSubmissionDeadline(request.submissionDeadline());
        }
        app.setUpdatedAt(Instant.now());

        ScholarshipApplication saved = applicationRepository.save(app);
        return ApplicationResponse.fromEntity(saved);
    }

    @Override
    public ApplicationResponse updateApplicationStatus(UUID id, UserPrincipal principal, UpdateStatusRequest request) {
        ScholarshipApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + id));

        checkAccess(app, principal);

        ApplicationStatus current = app.getCurrentStatus();
        ApplicationStatus target = request.status();

        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }

        app.setCurrentStatus(target);
        if (target == ApplicationStatus.SUBMITTED) {
            app.setActualSubmissionDate(request.actualSubmissionDate() != null ? request.actualSubmissionDate() : LocalDate.now());
            app.setSubmittedAt(Instant.now());
            app.setProgressPercentage(100);
        }
        if (target == ApplicationStatus.AWARDED && request.awardAmountReceived() != null) {
            app.setAwardAmountReceived(request.awardAmountReceived());
        }
        if (request.notes() != null) {
            app.setNotes(request.notes());
        }
        app.setUpdatedAt(Instant.now());

        ScholarshipApplication saved = applicationRepository.save(app);

        ApplicationStatusHistory history = new ApplicationStatusHistory(
                saved.getId(), current, target, principal.getId(), request.notes()
        );
        historyRepository.save(history);

        log.info("Transitioned application {} from {} to {}", id, current, target);
        return ApplicationResponse.fromEntity(saved);
    }

    @Override
    public ApplicationResponse withdrawApplication(UUID id, UserPrincipal principal, String notes) {
        return updateApplicationStatus(id, principal, new UpdateStatusRequest(
                ApplicationStatus.WITHDRAWN,
                notes != null ? notes : "Application withdrawn by applicant",
                null,
                null
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistory(UUID id, UserPrincipal principal) {
        ScholarshipApplication app = applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Application not found: " + id));
        checkAccess(app, principal);

        return historyRepository.findByApplicationIdOrderByChangedAtAsc(id)
                .stream()
                .map(StatusHistoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getUpcomingDeadlines(UUID studentId, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);
        return applicationRepository.findUpcomingActiveDeadlines(studentId, today, endDate)
                .stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    private void checkAccess(ScholarshipApplication app, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (principal.isInternal() || principal.hasRole("ROLE_ADMIN") || principal.hasRole("ROLE_SCHOLARSHIP_OFFICER")) {
            return;
        }
        if (!app.getStudentId().equals(principal.getId())) {
            throw new AccessDeniedException("Unauthorized to access application: " + app.getId());
        }
    }
}
