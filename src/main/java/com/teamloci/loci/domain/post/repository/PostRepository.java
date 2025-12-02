package com.teamloci.loci.domain.post.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.teamloci.loci.domain.post.entity.Post;
import com.teamloci.loci.domain.post.entity.PostStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.mediaList " +
            "LEFT JOIN FETCH p.collaborators c LEFT JOIN FETCH c.user " +
            "WHERE p.id = :postId")
    Optional<Post> findByIdWithDetails(@Param("postId") Long postId);

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.mediaList " +
            "WHERE p.beaconId = :beaconId AND p.status = 'ACTIVE' " +
            "ORDER BY p.id DESC")
    List<Post> findByBeaconId(@Param("beaconId") String beaconId);

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.user.id = :userId AND p.status = 'ACTIVE' " +
            "AND (:cursorId IS NULL OR p.id < :cursorId) " +
            "ORDER BY p.id DESC")
    List<Post> findByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.user.id = :userId AND p.status = 'ARCHIVED' " +
            "AND (:cursorId IS NULL OR p.id < :cursorId) " +
            "ORDER BY p.id DESC")
    List<Post> findArchivedPostsByUserIdWithCursor(
            @Param("userId") Long userId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.user " +
            "WHERE p.user.id IN :userIds " +
            "AND p.status = 'ACTIVE' " +
            "AND (:cursorId IS NULL OR p.id < :cursorId) " +
            "ORDER BY p.id DESC")
    List<Post> findByUserIdInWithCursor(
            @Param("userIds") List<Long> userIds,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query(value = "SELECT p.beacon_id, COUNT(*), " +
            "(" +
            "   SELECT p2.thumbnail_url " +
            "   FROM posts p2 " +
            "   WHERE p2.beacon_id = p.beacon_id " +
            "   AND p2.status = 'ACTIVE' " +
            "   AND (p2.user_id = :myUserId OR p2.user_id IN (" +
            "       SELECT f.receiver_id FROM friendships f WHERE f.requester_id = :myUserId AND f.status = 'FRIENDSHIP' " +
            "       UNION " +
            "       SELECT f.requester_id FROM friendships f WHERE f.receiver_id = :myUserId AND f.status = 'FRIENDSHIP'" +
            "   )) " +
            "   ORDER BY p2.id DESC " +
            "   LIMIT 1" +
            ") as thumbnail_url " +
            "FROM posts p " +
            "WHERE p.latitude BETWEEN :minLat AND :maxLat " +
            "AND p.longitude BETWEEN :minLon AND :maxLon " +
            "AND p.status = 'ACTIVE' " +
            "AND (p.user_id = :myUserId OR p.user_id IN (" +
            "   SELECT f.receiver_id FROM friendships f WHERE f.requester_id = :myUserId AND f.status = 'FRIENDSHIP' " +
            "   UNION " +
            "   SELECT f.requester_id FROM friendships f WHERE f.receiver_id = :myUserId AND f.status = 'FRIENDSHIP'" +
            ")) " +
            "GROUP BY p.beacon_id", nativeQuery = true)
    List<Object[]> findMapMarkers(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon,
            @Param("myUserId") Long myUserId
    );

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.mediaList " +
            "WHERE p.id IN (" +
            "  SELECT MAX(p2.id) FROM Post p2 " +
            "  WHERE p2.user.id IN :userIds AND p2.status = 'ACTIVE' " +
            "  GROUP BY p2.user.id" +
            ")")
    List<Post> findLatestPostsByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.status = 'ARCHIVED' " +
            "WHERE p.status = 'ACTIVE' " +
            "AND p.isArchived = true " +
            "AND p.createdAt < :expiryDate")
    int archiveOldPosts(@Param("expiryDate") LocalDateTime expiryDate);

    long countByUserIdAndStatus(Long userId, PostStatus status);

    @Query("SELECT p.user.id, COUNT(p) FROM Post p " +
            "WHERE p.user.id IN :userIds AND p.status = :status " +
            "GROUP BY p.user.id")
    List<Object[]> countPostsByUserIds(@Param("userIds") List<Long> userIds, @Param("status") PostStatus status);
}