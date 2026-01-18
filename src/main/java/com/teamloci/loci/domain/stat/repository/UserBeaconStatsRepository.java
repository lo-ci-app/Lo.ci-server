package com.teamloci.loci.domain.stat.repository;

import com.teamloci.loci.domain.stat.entity.UserBeaconStats;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBeaconStatsRepository extends JpaRepository<UserBeaconStats, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserBeaconStats> findByUserIdAndBeaconId(Long userId, String beaconId);

    @Query(value = """
        SELECT 
            s.beacon_id, 
            SUM(s.post_count),
            (
                SELECT inner_s.latest_thumbnail_url
                FROM user_beacon_stats inner_s
                WHERE inner_s.beacon_id = s.beacon_id
                AND inner_s.user_id IN :friendIds
                ORDER BY inner_s.latest_posted_at DESC
                LIMIT 1
            ) as thumbnail_url,
            MAX(s.latest_posted_at) as latest_posted_at
        FROM user_beacon_stats s
        WHERE s.user_id IN :friendIds
        AND s.latitude BETWEEN :minLat AND :maxLat
        AND s.longitude BETWEEN :minLon AND :maxLon
        GROUP BY s.beacon_id
        ORDER BY latest_posted_at DESC  
        LIMIT :limit                    
    """, nativeQuery = true)
    List<Object[]> findMarkersByFriendsInArea(
            @Param("friendIds") List<Long> friendIds,
            @Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon, @Param("maxLon") Double maxLon,
            @Param("limit") int limit
    );
}