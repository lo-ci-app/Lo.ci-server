package com.teamloci.loci.domain.post.repository;

import com.teamloci.loci.domain.post.entity.PostReaction;
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
}