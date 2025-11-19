package com.teamloci.loci.repository;

import com.teamloci.loci.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.mediaList " +
            "LEFT JOIN FETCH p.collaborators c LEFT JOIN FETCH c.user " +
            "WHERE p.id = :postId")
    Optional<Post> findByIdWithDetails(@Param("postId") Long postId);

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.user.id = :userId " +
            "ORDER BY p.createdAt DESC")
    List<Post> findByUserIdWithUser(@Param("userId") Long userId);
}