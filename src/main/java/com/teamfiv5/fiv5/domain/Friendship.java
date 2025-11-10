package com.teamfiv5.fiv5.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "friendships", uniqueConstraints = {
        // A가 B에게 보낸 요청은 유일해야 함 (중복 요청 방지)
        @UniqueConstraint(columnNames = {"requester_id", "receiver_id"})
})
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청 보낸 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false) // (복구) nullable = false
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false) // (복구) nullable = false
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendshipStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Friendship(User requester, User receiver, FriendshipStatus status) {
        this.requester = requester;
        this.receiver = receiver;
        this.status = status;
    }

    // (추가) 요청 수락
    public void accept() {
        this.status = FriendshipStatus.FRIENDSHIP;
    }
}