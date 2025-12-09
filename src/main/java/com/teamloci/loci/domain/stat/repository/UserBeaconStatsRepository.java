package com.teamloci.loci.domain.stat.repository;

import com.teamloci.loci.domain.stat.entity.UserBeaconStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBeaconStatsRepository extends JpaRepository<UserBeaconStats, Long> {

    Optional<UserBeaconStats> findByUserIdAndBeaconId(Long userId, String beaconId);

    @Query("SELECT s.beaconId, SUM(s.postCount), MAX(s.latestThumbnailUrl) " +
            "FROM UserBeaconStats s " +
            "WHERE s.userId IN :friendIds " +
            "AND s.latitude BETWEEN :minLat AND :maxLat " +
            "AND s.longitude BETWEEN :minLon AND :maxLon " +
            "GROUP BY s.beaconId")
    List<Object[]> findMarkersByFriendsInArea(
            @Param("friendIds") List<Long> friendIds,
            @Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon, @Param("maxLon") Double maxLon
    );
}