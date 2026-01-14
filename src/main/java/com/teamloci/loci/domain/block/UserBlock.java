package com.teamloci.loci.domain.block;

import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_block",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_block_blocker_blocked", // 제약조건 이름 명시
                        columnNames = {"blocker_id", "blocked_id"}
                )
        }
)
public class UserBlock extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private User blocked;

    @Builder
    public UserBlock(User blocker, User blocked) {
        this.blocker = blocker;
        this.blocked = blocked;
    }
}