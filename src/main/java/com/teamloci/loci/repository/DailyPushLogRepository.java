package com.teamloci.loci.repository;

import com.teamloci.loci.domain.DailyPushLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DailyPushLogRepository extends JpaRepository<DailyPushLog, String> {

    @Modifying
    @Query(value = "TRUNCATE TABLE daily_push_logs", nativeQuery = true)
    void truncateTable();
}