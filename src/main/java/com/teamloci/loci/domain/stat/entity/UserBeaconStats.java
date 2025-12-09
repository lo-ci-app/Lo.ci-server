package com.teamloci.loci.domain.stat.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_beacon_stats", indexes = {
        @Index(name = "idx_user_beacon", columnList = "user_id, beacon_id"),
        @Index(name = "idx_location", columnList = "latitude, longitude"),
        @Index(name = "idx_beacon_user_posted_at", columnList = "beacon_id, user_id, latest_posted_at")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "beacon_id"})
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

    private LocalDateTime latestPostedAt;

    @Builder
    public UserBeaconStats(Long userId, String beaconId, Double latitude, Double longitude, Long postCount, String latestThumbnailUrl, LocalDateTime latestPostedAt) {
        this.userId = userId;
        this.beaconId = beaconId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.postCount = postCount;
        this.latestThumbnailUrl = latestThumbnailUrl;
        this.latestPostedAt = latestPostedAt;
    }

    public void updateStats(String newThumbnailUrl, LocalDateTime postedAt) {
        this.postCount++;

        if (this.latestPostedAt == null || (postedAt != null && postedAt.isAfter(this.latestPostedAt))) {
            this.latestPostedAt = postedAt;
            if (newThumbnailUrl != null) {
                this.latestThumbnailUrl = newThumbnailUrl;
            }
        }
    }
}