package com.teamloci.loci.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "posts")
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = true, columnDefinition = "TEXT")
    private String contents;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PostMedia> mediaList = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PostCollaborator> collaborators = new HashSet<>();

    @Builder
    public Post(User user, String contents) {
        this.user = user;
        this.contents = contents;
    }

    public void addMedia(PostMedia media) {
        this.mediaList.add(media);
        media.setPost(this);
    }

    public void addCollaborator(PostCollaborator collaborator) {
        this.collaborators.add(collaborator);
        collaborator.setPost(this);
    }

    public void updateContents(String contents) {
        this.contents = contents;
    }

    public void clearMedia() {
        this.mediaList.clear();
    }

    public void clearCollaborators() {
        this.collaborators.clear();
    }
}