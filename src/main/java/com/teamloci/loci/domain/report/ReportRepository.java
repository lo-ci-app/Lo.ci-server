package com.teamloci.loci.domain.report;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {
    long countByTargetTypeAndTargetIdAndStatus(ReportTarget targetType, Long targetId, ReportStatus status);

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTarget targetType, Long targetId);

    @Modifying
    @Query("UPDATE Report r SET r.status = :newStatus WHERE r.targetType = :targetType AND r.targetId = :targetId AND r.status = :oldStatus")
    void updateStatusByTarget(@Param("targetType") ReportTarget targetType,
                              @Param("targetId") Long targetId,
                              @Param("oldStatus") ReportStatus oldStatus,
                              @Param("newStatus") ReportStatus newStatus);
}