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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posts", indexes = {
        @Index(name = "idx_beacon_id", columnList = "beacon_id")
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
    private boolean isArchived;

    @Builder
    public Post(User user, Double latitude, Double longitude, String locationName, String beaconId, Boolean isArchived, String thumbnailUrl) {
        this.user = user;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.beaconId = beaconId;
        this.status = PostStatus.ACTIVE;
        this.isArchived = (isArchived != null) ? isArchived : true;
        this.thumbnailUrl = thumbnailUrl;
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

    public void update(Double latitude, Double longitude, String locationName, String beaconId, Boolean isArchived, String thumbnailUrl) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationName = locationName;
        this.beaconId = beaconId;
        if (isArchived != null) {
            this.isArchived = isArchived;
        }
        this.thumbnailUrl = thumbnailUrl;
    }

    public void archive() {
        this.status = PostStatus.ARCHIVED;
    }

    public void restore() {
        this.status = PostStatus.ACTIVE;
        this.isArchived = false;
    }
}