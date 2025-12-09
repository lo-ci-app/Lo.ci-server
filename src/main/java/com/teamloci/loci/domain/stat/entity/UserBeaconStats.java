package com.teamloci.loci.domain.stat.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_beacon_stats", indexes = {
        @Index(name = "idx_user_beacon", columnList = "user_id, beacon_id"),
        @Index(name = "idx_location", columnList = "latitude, longitude")
})
public class UserBeaconStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "beacon_id", nullable = false)
    private String beaconId;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private Long postCount;

    @Column(columnDefinition = "TEXT")
    private String latestThumbnailUrl;

    @Builder
    public UserBeaconStats(Long userId, String beaconId, Double latitude, Double longitude, Long postCount, String latestThumbnailUrl) {
        this.userId = userId;
        this.beaconId = beaconId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.postCount = postCount;
        this.latestThumbnailUrl = latestThumbnailUrl;
    }

    public void incrementCount(String newThumbnailUrl) {
        this.postCount++;
        if (newThumbnailUrl != null) {
            this.latestThumbnailUrl = newThumbnailUrl;
        }
    }
}