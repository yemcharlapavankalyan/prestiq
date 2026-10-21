package org.prestiq.application.repository;

import org.prestiq.application.domain.entity.SavedScholarship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SavedScholarshipRepository extends JpaRepository<SavedScholarship, Long> {

    Page<SavedScholarship> findByStudentId(UUID studentId, Pageable pageable);

    Optional<SavedScholarship> findByStudentIdAndScholarshipId(UUID studentId, Long scholarshipId);

    boolean existsByStudentIdAndScholarshipId(UUID studentId, Long scholarshipId);

    void deleteByStudentIdAndScholarshipId(UUID studentId, Long scholarshipId);
}
