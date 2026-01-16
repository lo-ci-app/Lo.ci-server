package com.teamloci.loci.domain.post.repository;

import com.teamloci.loci.domain.post.entity.PostComment;
import com.teamloci.loci.domain.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    @Query("SELECT c FROM PostComment c " +
            "JOIN FETCH c.user " +
            "WHERE c.post.id = :postId " +
            "AND (:cursorId IS NULL OR c.id > :cursorId) " +
            "ORDER BY c.id ASC")
    List<PostComment> findByPostIdWithCursor(
            @Param("postId") Long postId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    long countByPostId(Long postId);

    @Query("SELECT c.post.id, COUNT(c) FROM PostComment c WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<Object[]> countByPostIdIn(@Param("postIds") List<Long> postIds);

    long countByUser(User user);

    void deleteByUser(User user);
}