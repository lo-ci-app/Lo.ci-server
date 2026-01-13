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
@Table(name = "friendship_intimacies",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id_a", "user_id_b"})
        }
)
public class FriendshipIntimacy extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id_a", nullable = false)
    private Long userAId;

    @Column(name = "user_id_b", nullable = false)
    private Long userBId;

    @Column(nullable = false)
    private Long totalScore;

    @Column(nullable = false)
    private Integer level;

    @Builder
    public FriendshipIntimacy(Long user1Id, Long user2Id) {
        if (user1Id < user2Id) {
            this.userAId = user1Id;
            this.userBId = user2Id;
        } else {
            this.userAId = user2Id;
            this.userBId = user1Id;
        }
        this.totalScore = 0L;
        this.level = 1;
    }

    public void addScore(int score) {
        this.totalScore += score;
    }

    public void updateLevel(int level) {
        this.level = level;
    }
}