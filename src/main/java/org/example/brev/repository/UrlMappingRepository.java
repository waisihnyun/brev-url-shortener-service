package org.example.brev.repository;

import org.example.brev.entity.UrlMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    /**
     * Find URL mapping by short code
     */
    Optional<UrlMapping> findByShortCode(String shortCode);

    /**
     * Find URL mapping by long URL
     */
    Optional<UrlMapping> findByLongUrl(String longUrl);

    /**
     * Check if short code already exists
     */
    boolean existsByShortCode(String shortCode);

    /**
     * Find all URL mappings created after a specific date
     */
    List<UrlMapping> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Find all URL mappings created between two dates
     */
    List<UrlMapping> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Delete URL mappings older than specified date
     */
    @Modifying
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime date);
}
