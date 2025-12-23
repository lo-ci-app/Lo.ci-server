package com.teamloci.loci.domain.report;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByTargetTypeAndTargetIdAndStatus(ReportTarget targetType, Long targetId, ReportStatus status);

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTarget targetType, Long targetId);
}