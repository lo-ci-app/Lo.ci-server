package com.teamloci.loci.domain.post.repository;

import com.teamloci.loci.domain.post.entity.PostReaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT r.post.id, r.type, COUNT(r) FROM PostReaction r WHERE r.post.id IN :postIds GROUP BY r.post.id, r.type")
    List<Object[]> countReactionsByPostIds(@Param("postIds") List<Long> postIds);

    @Query("SELECT r.post.id, r.type FROM PostReaction r WHERE r.post.id IN :postIds AND r.user.id = :userId")
    List<Object[]> findMyReactions(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);

    @Query("SELECT r FROM PostReaction r " +
            "JOIN FETCH r.user " +
            "WHERE r.post.id = :postId " +
            "AND (:cursorId IS NULL OR r.id < :cursorId) " +
            "ORDER BY r.id DESC")
    List<PostReaction> findByPostIdWithCursor(@Param("postId") Long postId, @Param("cursorId") Long cursorId, Pageable pageable);

    @Query("SELECT r FROM PostReaction r " +
            "JOIN FETCH r.user " +
            "WHERE r.post.id = :postId " +
            "AND r.user.id != :userId " +
            "AND (:cursorId IS NULL OR r.id < :cursorId) " +
            "ORDER BY r.id DESC")
    List<PostReaction> findByPostIdAndUserIdNotWithCursor(
            @Param("postId") Long postId,
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}