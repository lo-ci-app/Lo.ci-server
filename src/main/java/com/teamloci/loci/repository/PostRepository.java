package com.teamloci.loci.repository;

import com.teamloci.loci.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
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

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN FETCH p.mediaList " +
            "WHERE p.beaconId = :beaconId " +
            "ORDER BY p.createdAt DESC")
    List<Post> findByBeaconId(@Param("beaconId") String beaconId);

    @Query("SELECT p.beaconId, COUNT(p), MAX(pm.mediaUrl) " +
            "FROM Post p " +
            "LEFT JOIN p.mediaList pm " +
            "WHERE p.latitude BETWEEN :minLat AND :maxLat " +
            "AND p.longitude BETWEEN :minLon AND :maxLon " +
            "GROUP BY p.beaconId")
    List<Object[]> findMapMarkers(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.user.id IN (" +
            "SELECT f.receiver.id FROM Friendship f WHERE f.requester.id = :myUserId AND f.status = 'FRIENDSHIP' " +
            "UNION " +
            "SELECT f.requester.id FROM Friendship f WHERE f.receiver.id = :myUserId AND f.status = 'FRIENDSHIP'" +
            ") " +
            "AND p.createdAt < :lastCreatedAt " +
            "ORDER BY p.createdAt DESC")
    List<Post> findFriendPosts(
            @Param("myUserId") Long myUserId,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            Pageable pageable
    );

    @Query("SELECT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "WHERE p.user.id IN (" +
            "SELECT f.receiver.id FROM Friendship f WHERE f.requester.id = :myUserId AND f.status = 'FRIENDSHIP' " +
            "UNION " +
            "SELECT f.requester.id FROM Friendship f WHERE f.receiver.id = :myUserId AND f.status = 'FRIENDSHIP'" +
            ") " +
            "ORDER BY p.createdAt DESC")
    List<Post> findFriendPostsFirstPage(
            @Param("myUserId") Long myUserId,
            Pageable pageable
    );
}