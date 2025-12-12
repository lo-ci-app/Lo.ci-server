package com.teamloci.loci.domain.notification;

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
@Table(name = "notifications", indexes = {
        @Index(name = "idx_receiver_id", columnList = "receiver_id")
})
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(nullable = false)
    private boolean isRead;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Builder
    public Notification(User receiver, String title, String body, NotificationType type, Long relatedId, String thumbnailUrl) {
        this.receiver = receiver;
        this.title = title;
        this.body = body;
        this.type = type;
        this.relatedId = relatedId;
        this.thumbnailUrl = thumbnailUrl;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}