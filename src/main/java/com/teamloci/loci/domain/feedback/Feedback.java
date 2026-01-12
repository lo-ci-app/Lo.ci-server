package com.teamloci.loci.domain.feedback;

import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Feedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private FeedbackStatus status = FeedbackStatus.PENDING; // 대기중, 완료됨
}