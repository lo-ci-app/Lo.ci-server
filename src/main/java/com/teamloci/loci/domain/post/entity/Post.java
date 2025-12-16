package com.teamloci.loci.domain.post.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posts", indexes = {
        @Index(name = "idx_beacon_id", columnList = "beacon_id"),
        @Index(name = "idx_post_feed", columnList = "user_id, status, id DESC")
})
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @BatchSize(size = 100)
    private List<PostMedia> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostCollaborator> collaborators = new HashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PostReaction> reactions = new ArrayList<>();

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = true)
    private String locationName;

    @Column(name = "beacon_id", nullable = false, length = 64)
    private String beaconId;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostStatus status;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long commentCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long reactionCount = 0;

    @Column(length = 16)
    private String description;

    @Builder
    public Post(User user, Double latitude, Double longitude, String locationName, String beaconId, String thumbnailUrl, String description) {
        this.user = user;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.beaconId = beaconId;
        this.status = PostStatus.ACTIVE;
        this.thumbnailUrl = thumbnailUrl;
        this.description = description;
    }

    public void addMedia(PostMedia media) {
        this.mediaList.add(media);
        media.setPost(this);
    }

    public void addCollaborator(PostCollaborator collaborator) {
        this.collaborators.add(collaborator);
        collaborator.setPost(this);
    }

    public void clearMedia() {
        this.mediaList.clear();
    }

    public void clearCollaborators() {
        this.collaborators.clear();
    }

    public void update(Double latitude, Double longitude, String locationName, String beaconId, String thumbnailUrl) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.beaconId = beaconId;
        this.thumbnailUrl = thumbnailUrl;
    }

    public void archive() {
        this.status = PostStatus.ARCHIVED;
    }

    public void restore() {
        this.status = PostStatus.ACTIVE;
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}