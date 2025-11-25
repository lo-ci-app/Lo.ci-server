package com.teamloci.loci.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamloci.loci.domain.Post;

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
            "WHERE (p.user.id = :myUserId OR p.user.id IN (" +
            "   SELECT f.receiver.id FROM Friendship f WHERE f.requester.id = :myUserId AND f.status = 'FRIENDSHIP' " +
            "   UNION " +
            "   SELECT f.requester.id FROM Friendship f WHERE f.receiver.id = :myUserId AND f.status = 'FRIENDSHIP'" +
            ")) " +
            "AND p.status = 'ACTIVE' " +
            "AND (:cursorId IS NULL OR p.id < :cursorId) " +
            "ORDER BY p.id DESC")
    List<Post> findFriendPostsWithCursor(
            @Param("myUserId") Long myUserId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query(value = "SELECT p.beacon_id, COUNT(*), " +
            "(SELECT pm.media_url " +
            " FROM post_media pm " +
            " JOIN posts p2 ON pm.post_id = p2.id " +
            " WHERE p2.beacon_id = p.beacon_id " +
            "   AND p2.status = 'ACTIVE' " +
            " ORDER BY p2.created_at DESC, pm.sort_order ASC " +
            " LIMIT 1) as thumbnail_url " +
            "FROM posts p " +
            "WHERE p.latitude BETWEEN :minLat AND :maxLat " +
            "AND p.longitude BETWEEN :minLon AND :maxLon " +
            "AND p.status = 'ACTIVE' " +
            "GROUP BY p.beacon_id", nativeQuery = true)
    List<Object[]> findMapMarkers(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.status = 'ARCHIVED' " +
            "WHERE p.status = 'ACTIVE' " +
            "AND p.isArchived = true " +
            "AND p.createdAt < :expiryDate")
    int archiveOldPosts(@Param("expiryDate") LocalDateTime expiryDate);
}