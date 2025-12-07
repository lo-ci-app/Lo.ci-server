package com.teamloci.loci.domain.intimacy.entity;

import com.teamloci.loci.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "intimacy_logs", indexes = {
        @Index(name = "idx_intimacy_log_check", columnList = "actor_id, target_id, type, created_at")
})
public class IntimacyLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntimacyType type;

    @Column(nullable = false)
    private int earnedPoint;

    @Column(name = "related_beacon_id")
    private String relatedBeaconId;

    @Builder
    public IntimacyLog(Long actorId, Long targetId, IntimacyType type, int earnedPoint, String relatedBeaconId) {
        this.actorId = actorId;
        this.targetId = targetId;
        this.type = type;
        this.earnedPoint = earnedPoint;
        this.relatedBeaconId = relatedBeaconId;
    }
}