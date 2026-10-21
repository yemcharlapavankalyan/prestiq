package org.prestiq.application.service;

import org.prestiq.application.client.NotificationServiceClient;
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
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationServiceImpl.class);
    private static final List<ApplicationStatus> TERMINAL_STATUSES = List.of(
            ApplicationStatus.WITHDRAWN,
            ApplicationStatus.REJECTED
    );

    private final ScholarshipApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ScholarshipServiceClient scholarshipServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    public ApplicationServiceImpl(
            ScholarshipApplicationRepository applicationRepository,
            ApplicationStatusHistoryRepository statusHistoryRepository,
            ScholarshipServiceClient scholarshipServiceClient,
            NotificationServiceClient notificationServiceClient
    ) {
        this.applicationRepository = applicationRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.scholarshipServiceClient = scholarshipServiceClient;
        this.notificationServiceClient = notificationServiceClient;
    }

    @Override
    @Transactional
    public ApplicationResponse createApplication(UUID studentId, CreateApplicationRequest request) {
        // 1. Enforce Sensible Duplicate Rules:
        // A student cannot have multiple concurrent active applications for the same scholarship.
        // If an earlier application was WITHDRAWN or REJECTED, they can initiate a new application.
        if (applicationRepository.existsByStudentIdAndScholarshipIdAndCurrentStatusNotIn(
                studentId, request.scholarshipId(), TERMINAL_STATUSES)) {
            throw new DuplicateApplicationException(
                    "An active application already exists for scholarship ID: " + request.scholarshipId() +
                    ". You cannot create a duplicate application while an active application is in progress."
            );
        }

        // 2. Fetch Verified Scholarship Snapshot from Scholarship Service
        String title = request.scholarshipTitle();
        String provider = request.scholarshipProvider();
        LocalDate deadline = request.submissionDeadline();

        Optional<ScholarshipClientDto> scholOpt = scholarshipServiceClient.fetchScholarshipById(request.scholarshipId());
        if (scholOpt.isPresent()) {
            ScholarshipClientDto dto = scholOpt.get();
            if (!StringUtils.hasText(title)) title = dto.title();
            if (!StringUtils.hasText(provider)) provider = dto.providerName();
            if (deadline == null) deadline = dto.applicationDeadline();
        }

        if (!StringUtils.hasText(title)) {
            title = "Scholarship #" + request.scholarshipId();
        }
        if (deadline == null) {
            deadline = LocalDate.now().plusMonths(1);
        }

        // 3. Persist Application
        ScholarshipApplication application = new ScholarshipApplication(
                UUID.randomUUID(),
                studentId,
                request.scholarshipId(),
                title,
                provider,
                ApplicationStatus.PLANNING,
                request.externalPortalReference(),
                deadline,
                null,
                null,
                null,
                0,
                request.notes(),
                Instant.now(),
                Instant.now()
        );

        ScholarshipApplication saved = applicationRepository.save(application);
        log.info("Created new scholarship application {} for student {} on scholarship {}",
                saved.getId(), studentId, request.scholarshipId());

        // 4. Record Initial Status Transition History
        ApplicationStatusHistory history = new ApplicationStatusHistory(
                saved.getId(),
                null,
                ApplicationStatus.PLANNING,
                studentId,
                "Application started in PLANNING stage"
        );
        statusHistoryRepository.save(history);

        // 5. Schedule Deadline Reminder with Notification Service
        try {
            notificationServiceClient.scheduleDeadlineReminder(saved.getId(), studentId, deadline);
        } catch (Exception e) {
            log.warn("Could not dispatch deadline reminder for application {}: {}", saved.getId(), e.getMessage());
        }

        return ApplicationResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getApplications(
            UUID studentId,
            ApplicationStatus status,
            Pageable pageable
    ) {
        Page<ScholarshipApplication> page = (status != null)
                ? applicationRepository.findByStudentIdAndCurrentStatus(studentId, status, pageable)
                : applicationRepository.findByStudentId(studentId, pageable);

        return PagedResponse.fromPage(page.map(ApplicationResponse::fromEntity));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationById(UUID id, UserPrincipal principal) {
        ScholarshipApplication app = findApplicationOrThrow(id);
        verifyAccess(app, principal);
        return ApplicationResponse.fromEntity(app);
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplication(
            UUID id,
            UserPrincipal principal,
            UpdateApplicationRequest request
    ) {
        ScholarshipApplication app = findApplicationOrThrow(id);
        verifyAccess(app, principal);

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
            // Reschedule notification on deadline adjustment
            notificationServiceClient.scheduleDeadlineReminder(app.getId(), app.getStudentId(), request.submissionDeadline());
        }

        app.setUpdatedAt(Instant.now());
        ScholarshipApplication saved = applicationRepository.save(app);
        log.info("Updated progress/notes for application {}", id);
        return ApplicationResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ApplicationResponse updateApplicationStatus(
            UUID id,
            UserPrincipal principal,
            UpdateStatusRequest request
    ) {
        ScholarshipApplication app = findApplicationOrThrow(id);
        verifyAccess(app, principal);

        ApplicationStatus fromStatus = app.getCurrentStatus();
        ApplicationStatus toStatus = request.status();

        // Validate allowed state transition
        if (!fromStatus.canTransitionTo(toStatus)) {
            throw new InvalidStatusTransitionException(fromStatus, toStatus);
        }

        app.setCurrentStatus(toStatus);
        app.setUpdatedAt(Instant.now());

        // Specialized fields for submission and award
        if (toStatus == ApplicationStatus.SUBMITTED) {
            app.setSubmittedAt(Instant.now());
            app.setActualSubmissionDate(
                    request.actualSubmissionDate() != null ? request.actualSubmissionDate() : LocalDate.now()
            );
            app.setProgressPercentage(100);
        }

        if (toStatus == ApplicationStatus.AWARDED && request.awardAmountReceived() != null) {
            app.setAwardAmountReceived(request.awardAmountReceived());
        }

        ScholarshipApplication saved = applicationRepository.save(app);

        // Record audit transition history
        ApplicationStatusHistory history = new ApplicationStatusHistory(
                app.getId(),
                fromStatus,
                toStatus,
                principal.getId(),
                request.notes()
        );
        statusHistoryRepository.save(history);

        log.info("Transitioned application {} status from {} to {}", id, fromStatus, toStatus);
        return ApplicationResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ApplicationResponse withdrawApplication(
            UUID id,
            UserPrincipal principal,
            String notes
    ) {
        UpdateStatusRequest req = new UpdateStatusRequest(
                ApplicationStatus.WITHDRAWN,
                notes != null ? notes : "Application voluntarily withdrawn by student",
                null,
                null
        );
        return updateApplicationStatus(id, principal, req);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getStatusHistory(UUID id, UserPrincipal principal) {
        ScholarshipApplication app = findApplicationOrThrow(id);
        verifyAccess(app, principal);

        return statusHistoryRepository.findByApplicationIdOrderByChangedAtAsc(id).stream()
                .map(StatusHistoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getUpcomingDeadlines(UUID studentId, int daysAhead) {
        LocalDate today = LocalDate.now();
        List<ScholarshipApplication> list;
        if (daysAhead > 0) {
            LocalDate maxDate = today.plusDays(daysAhead);
            list = applicationRepository.findUpcomingActiveDeadlines(studentId, today, maxDate);
        } else {
            list = applicationRepository.findAllUpcomingActiveDeadlines(studentId, today);
        }

        return list.stream()
                .map(ApplicationResponse::fromEntity)
                .toList();
    }

    private ScholarshipApplication findApplicationOrThrow(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new ApplicationNotFoundException("Scholarship application not found with ID: " + id));
    }

    private void verifyAccess(ScholarshipApplication app, UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication required to access this application");
        }
        if (!app.getStudentId().equals(principal.getId()) && !principal.hasRole("ADMIN") && !principal.isInternal()) {
            throw new AccessDeniedException("Access denied: You do not have permission to access another student's application");
        }
    }
}
