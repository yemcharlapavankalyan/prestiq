package org.prestiq.application.repository;

import org.prestiq.application.domain.entity.ApplicationStatus;
import org.prestiq.application.domain.entity.ScholarshipApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScholarshipApplicationRepository extends JpaRepository<ScholarshipApplication, UUID> {

    Page<ScholarshipApplication> findByStudentId(UUID studentId, Pageable pageable);

    Page<ScholarshipApplication> findByStudentIdAndCurrentStatus(
            UUID studentId,
            ApplicationStatus currentStatus,
            Pageable pageable
    );

    Page<ScholarshipApplication> findByStudentIdAndCurrentStatusIn(
            UUID studentId,
            Collection<ApplicationStatus> statuses,
            Pageable pageable
    );

    Optional<ScholarshipApplication> findByIdAndStudentId(UUID id, UUID studentId);

    boolean existsByStudentIdAndScholarshipIdAndCurrentStatusNotIn(
            UUID studentId,
            Long scholarshipId,
            Collection<ApplicationStatus> excludedStatuses
    );

    Optional<ScholarshipApplication> findByStudentIdAndScholarshipIdAndCurrentStatusNotIn(
            UUID studentId,
            Long scholarshipId,
            Collection<ApplicationStatus> excludedStatuses
    );

    @Query("SELECT a FROM ScholarshipApplication a WHERE a.studentId = :studentId " +
           "AND a.currentStatus NOT IN ('AWARDED', 'REJECTED', 'WITHDRAWN') " +
           "AND a.submissionDeadline BETWEEN :startDate AND :endDate " +
           "ORDER BY a.submissionDeadline ASC")
    List<ScholarshipApplication> findUpcomingActiveDeadlines(
            @Param("studentId") UUID studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT a FROM ScholarshipApplication a WHERE a.studentId = :studentId " +
           "AND a.currentStatus NOT IN ('AWARDED', 'REJECTED', 'WITHDRAWN') " +
           "AND a.submissionDeadline >= :today " +
           "ORDER BY a.submissionDeadline ASC")
    List<ScholarshipApplication> findAllUpcomingActiveDeadlines(
            @Param("studentId") UUID studentId,
            @Param("today") LocalDate today
    );
}
