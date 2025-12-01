package com.teamloci.loci.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comment_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"comment_id", "user_id"})
})
public class CommentLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private PostComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    public CommentLike(PostComment comment, User user) {
        this.comment = comment;
        this.user = user;
    }
}