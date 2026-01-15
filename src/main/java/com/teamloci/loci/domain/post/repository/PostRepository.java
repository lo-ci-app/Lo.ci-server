package com.teamloci.loci.domain.post.repository;

import java.time.LocalDateTime;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

import com.teamloci.loci.domain.post.entity.Post;
import com.teamloci.loci.domain.post.entity.PostStatus;
import com.teamloci.loci.domain.user.User;
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
            "JOIN FETCH p.user " +
            "WHERE p.beaconId = :beaconId " +
            "AND ( " +
            "   (p.user.id = :myUserId AND (p.status = 'ACTIVE' OR p.status = 'ARCHIVED')) " +
            "   OR " +
            "   (p.user.id IN :friendIds AND p.status = 'ACTIVE') " +
            ") " +
            "AND (:cursorId IS NULL OR p.id < :cursorId) " +
            "ORDER BY p.id DESC")
    List<Post> findTimelinePostsWithCursor(@Param("beaconId") String beaconId,
                                           @Param("myUserId") Long myUserId,
                                           @Param("friendIds") List<Long> friendIds,
                                           @Param("cursorId") Long cursorId,
                                           Pageable pageable);

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN p.collaborators pc " +
            "WHERE (p.user.id = :userId OR pc.user.id = :userId) " +
            "AND p.status IN :statuses " +
            "AND (:cursorId IS NULL OR p.id < :cursorId) " +
            "ORDER BY p.id DESC")
    List<Post> findByUserIdAndStatusInWithCursor(
            @Param("userId") Long userId,
            @Param("statuses") List<PostStatus> statuses,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.user " +
            "LEFT JOIN p.collaborators pc " +
            "WHERE (p.user.id = :userId OR pc.user.id = :userId) " +
            "AND p.status = 'ARCHIVED' " +
            "AND (:cursorId IS NULL OR p.id < :cursorId) " +
            "ORDER BY p.id DESC")
    List<Post> findArchivedPostsByUserIdWithCursor(@Param("userId") Long userId, @Param("cursorId") Long cursorId, Pageable pageable);

    @Query("SELECT p FROM Post p " +
            "JOIN FETCH p.user " +
            "WHERE p.user.id IN :userIds " +
            "AND p.status = 'ACTIVE' " +
            "AND (:cursorId IS NULL OR p.id < :cursorId) " +
            "ORDER BY p.id DESC")
    List<Post> findByUserIdInWithCursor(@Param("userIds") List<Long> userIds, @Param("cursorId") Long cursorId, Pageable pageable);

    @Query(value = "SELECT p.beacon_id, COUNT(*), " +
            "(" +
            "   SELECT p2.thumbnail_url " +
            "   FROM posts p2 " +
            "   WHERE p2.beacon_id = p.beacon_id " +
            "   AND (p2.status = 'ACTIVE' OR (p2.user_id = :myUserId AND p2.status = 'ARCHIVED')) " +
            "   AND (p2.user_id = :myUserId OR p2.user_id IN (" +
            "       SELECT f.receiver_id FROM friendships f WHERE f.requester_id = :myUserId AND f.status = 'FRIENDSHIP' " +
            "       UNION " +
            "       SELECT f.requester_id FROM friendships f WHERE f.receiver_id = :myUserId AND f.status = 'FRIENDSHIP'" +
            "   )) " +
            "   AND p2.user_id NOT IN (SELECT blocked_id FROM user_block WHERE blocker_id = :myUserId) " + // 👈 차단 필터 추가
            "   ORDER BY p2.id DESC " +
            "   LIMIT 1" +
            ") as thumbnail_url, " +
            "MAX(p.created_at) as latest_posted_at " +
            "FROM posts p " +
            "WHERE p.latitude BETWEEN :minLat AND :maxLat " +
            "AND p.longitude BETWEEN :minLon AND :maxLon " +
            "AND (p.status = 'ACTIVE' OR (p.user_id = :myUserId AND p.status = 'ARCHIVED')) " +
            "AND (p.user_id = :myUserId OR p.user_id IN (" +
            "   SELECT f.receiver_id FROM friendships f WHERE f.requester_id = :myUserId AND f.status = 'FRIENDSHIP' " +
            "   UNION " +
            "   SELECT f.requester_id FROM friendships f WHERE f.receiver_id = :myUserId AND f.status = 'FRIENDSHIP'" +
            ")) " +
            "AND p.user_id NOT IN (SELECT blocked_id FROM user_block WHERE blocker_id = :myUserId) " + // 👈 차단 필터 추가
            "GROUP BY p.beacon_id", nativeQuery = true)
    List<Object[]> findMapMarkers(
            @Param("minLat") Double minLat, @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon, @Param("maxLon") Double maxLon,
            @Param("myUserId") Long myUserId
    );

    @Query("SELECT DISTINCT p FROM Post p " +
            "LEFT JOIN FETCH p.mediaList " +
            "WHERE p.id IN (" +
            "  SELECT MAX(p2.id) FROM Post p2 " +
            "  WHERE p2.user.id IN :userIds AND p2.status = 'ACTIVE' " +
            "  GROUP BY p2.user.id" +
            ")")
    List<Post> findLatestPostsByUserIds(@Param("userIds") List<Long> userIds);

    @Query(value = """
            WITH RankedPosts AS (
            SELECT
                p.id,
                p.beacon_id,
                p.location_name,
                p.thumbnail_url,
                p.created_at,
                COUNT(*) OVER (PARTITION BY p.beacon_id) as cnt,
                ROW_NUMBER() OVER (
                    PARTITION BY p.beacon_id 
                    ORDER BY 
                        -- [수정] 유효한 장소명을 우선순위로 두기 위한 정렬 로직 추가
                        CASE 
                            WHEN p.location_name IN ('Somewhere', 'Unknown') THEN 1 
                            ELSE 0 
                        END ASC,
                        p.created_at DESC, 
                        p.id DESC
                ) as rn
            FROM posts p
            WHERE p.user_id = :userId 
              AND (p.status = 'ACTIVE' OR p.status = 'ARCHIVED')
        )
        SELECT beacon_id, location_name, cnt, thumbnail_url, created_at, id
        FROM RankedPosts
        WHERE rn = 1
        ORDER BY created_at DESC
        """, nativeQuery = true)
    List<Object[]> findVisitedPlacesByUserId(@Param("userId") Long userId);

    @Query("""
        SELECT DISTINCT p.user 
        FROM Post p 
        WHERE p.beaconId IN :beaconIds 
          AND p.user.id IN :friendIds 
          AND p.status = 'ACTIVE'
        ORDER BY p.createdAt DESC
        """)
    List<User> findFriendsInBeacons(@Param("beaconIds") List<String> beaconIds,
                                    @Param("friendIds") List<Long> friendIds,
                                    Pageable pageable);

    @Query("SELECT COUNT(DISTINCT p.user) FROM Post p WHERE p.beaconId IN :beaconIds AND p.user.id IN :friendIds AND p.status = 'ACTIVE'")
    Long countFriendsInBeacons(@Param("beaconIds") List<String> beaconIds, @Param("friendIds") List<Long> friendIds);

    boolean existsByBeaconIdInAndUserId(List<String> beaconIds, Long userId);

    @Query("SELECT DISTINCT p.user FROM Post p " +
            "WHERE p.beaconId = :beaconId " +
            "AND p.user.id IN :friendIds")
    List<User> findUsersWhoPostedInBeacon(@Param("beaconId") String beaconId,
                                          @Param("friendIds") List<Long> friendIds);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Post p SET p.status = 'ARCHIVED' " +
            "WHERE p.status = 'ACTIVE' " +
            "AND p.createdAt < :expiryDate " +
            "AND p.user.id IN (SELECT u.id FROM User u WHERE u.isAutoArchive = true)")
    int archiveOldPosts(@Param("expiryDate") LocalDateTime expiryDate);

    long countByUserIdAndStatus(Long userId, PostStatus status);

    @Query("SELECT p.user.id, COUNT(p) FROM Post p " +
            "WHERE p.user.id IN :userIds AND p.status = :status " +
            "GROUP BY p.user.id")
    List<Object[]> countPostsByUserIds(@Param("userIds") List<Long> userIds, @Param("status") PostStatus status);

    @Query("SELECT DISTINCT p.user.id FROM Post p " +
            "WHERE p.createdAt >= :startDateTime " +
            "AND p.createdAt < :endDateTime")
    List<Long> findUserIdsWhoPostedBetween(@Param("startDateTime") LocalDateTime startDateTime,
                                           @Param("endDateTime") LocalDateTime endDateTime);

    boolean existsByBeaconIdAndUserId(String beaconId, Long userId);

    @Query(value = "SELECT DISTINCT DATE(created_at) FROM posts WHERE user_id = :userId AND status = 'ACTIVE' ORDER BY created_at DESC", nativeQuery = true)
    List<Date> findDistinctPostDates(@Param("userId") Long userId);

    @Query(value = "SELECT user_id, DATE(created_at) as post_date FROM posts WHERE user_id IN :userIds AND status = 'ACTIVE' ORDER BY user_id, created_at DESC", nativeQuery = true)
    List<Object[]> findPostDatesByUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT COUNT(DISTINCT p.beaconId) FROM Post p WHERE p.user.id = :userId AND (p.status = 'ACTIVE' OR p.status = 'ARCHIVED')")
    long countDistinctBeaconsByUserId(@Param("userId") Long userId);

    @Query("SELECT p.user.id, COUNT(DISTINCT p.beaconId) FROM Post p WHERE p.user.id IN :userIds AND (p.status = 'ACTIVE' OR p.status = 'ARCHIVED') GROUP BY p.user.id")
    List<Object[]> countDistinctBeaconsByUserIds(@Param("userIds") List<Long> userIds);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    void increaseCommentCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount - 1 WHERE p.id = :postId AND p.commentCount > 0")
    void decreaseCommentCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.reactionCount = p.reactionCount + 1 WHERE p.id = :postId")
    void increaseReactionCount(@Param("postId") Long postId);

    @Modifying
    @Query("UPDATE Post p SET p.reactionCount = p.reactionCount - 1 WHERE p.id = :postId AND p.reactionCount > 0")
    void decreaseReactionCount(@Param("postId") Long postId);

    long countByUserIdAndBeaconId(Long userId, String beaconId);

    @Query("SELECT p.user.id, p.createdAt FROM Post p WHERE p.createdAt BETWEEN :start AND :end AND p.status = 'ACTIVE'")
    List<Object[]> findPostTimestampsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    Optional<Post> findTopByUserIdAndBeaconIdOrderByIdDesc(Long userId, String beaconId);

    Optional<Post> findTopByUserIdOrderByIdDesc(Long userId);

    @Query(value = "SELECT COUNT(*) FROM post p " +
            "WHERE p.user_id = :userId " +
            "AND HOUR(CONVERT_TZ(p.created_at, '+00:00', :timeOffset)) >= :startHour " +
            "AND HOUR(CONVERT_TZ(p.created_at, '+00:00', :timeOffset)) < :endHour",
            nativeQuery = true)
    long countByUserAndCreatedHourBetween(
            @Param("userId") Long userId,
            @Param("startHour") int startHour,
            @Param("endHour") int endHour,
            @Param("timeOffset") String timeOffset);

    void deleteByUser(User user);

}