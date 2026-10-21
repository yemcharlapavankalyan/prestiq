package org.prestiq.scholarship.repository;

import org.prestiq.scholarship.domain.entity.PublicationStatus;
import org.prestiq.scholarship.domain.entity.Scholarship;
import org.prestiq.scholarship.domain.entity.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScholarshipRepository extends JpaRepository<Scholarship, Long>, JpaSpecificationExecutor<Scholarship> {

    Optional<Scholarship> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Scholarship> findByIdAndVerificationStatusAndPublicationStatus(
            Long id,
            VerificationStatus verificationStatus,
            PublicationStatus publicationStatus
    );

    Optional<Scholarship> findBySlugAndVerificationStatusAndPublicationStatus(
            String slug,
            VerificationStatus verificationStatus,
            PublicationStatus publicationStatus
    );
}
