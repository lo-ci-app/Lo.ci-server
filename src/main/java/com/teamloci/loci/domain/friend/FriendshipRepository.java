package com.teamloci.loci.domain.friend;

import com.teamloci.loci.domain.user.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.requester.id = :userId OR f.receiver.id = :userId)")
    List<Friendship> findAllFriendshipsByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.requester.id = :userA AND f.receiver.id = :userB) " +
            "OR (f.requester.id = :userB AND f.receiver.id = :userA)")
    Optional<Friendship> findFriendshipBetween(@Param("userA") Long userA, @Param("userB") Long userB);

    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.requester.id = :myUserId AND f.receiver.id IN :targetUserIds) " +
            "OR (f.receiver.id = :myUserId AND f.requester.id IN :targetUserIds)")
    List<Friendship> findAllRelationsBetween(@Param("myUserId") Long myUserId, @Param("targetUserIds") List<Long> targetUserIds);

    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.requester JOIN FETCH f.receiver " +
            "WHERE (f.requester.id = :userId OR f.receiver.id = :userId) " +
            "AND f.status = 'FRIENDSHIP'")
    List<Friendship> findAllFriendsWithUsers(@Param("userId") Long userId);

    @Query("SELECT f.receiver FROM Friendship f WHERE f.requester.id = :userId AND f.status = 'FRIENDSHIP' AND f.receiver.status = 'ACTIVE' " +
            "UNION " +
            "SELECT f.requester FROM Friendship f WHERE f.receiver.id = :userId AND f.status = 'FRIENDSHIP' AND f.requester.status = 'ACTIVE'")
    List<User> findActiveFriendsByUserId(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f JOIN FETCH f.requester " +
            "WHERE f.receiver.id = :userId AND f.status = 'PENDING'")
    List<Friendship> findReceivedRequests(@Param("userId") Long userId);

    @Query("SELECT f FROM Friendship f JOIN FETCH f.receiver " +
            "WHERE f.requester.id = :userId AND f.status = 'PENDING'")
    List<Friendship> findSentRequests(@Param("userId") Long userId);

    @Query("SELECT COUNT(f) FROM Friendship f " +
            "WHERE (f.requester.id = :userId OR f.receiver.id = :userId) " +
            "AND f.status = 'FRIENDSHIP'")
    long countFriends(@Param("userId") Long userId);

    @Query(value = """
        SELECT user_id, COUNT(*) 
        FROM (
            SELECT requester_id AS user_id FROM friendships WHERE requester_id IN :userIds AND status = 'FRIENDSHIP'
            UNION ALL
            SELECT receiver_id AS user_id FROM friendships WHERE receiver_id IN :userIds AND status = 'FRIENDSHIP'
        ) AS all_friends 
        GROUP BY user_id
        """, nativeQuery = true)
    List<Object[]> countFriendsByUserIds(@Param("userIds") List<Long> userIds);

    @Cacheable(value = "activeFriends", key = "#userId")
    @Query("SELECT f.receiver FROM Friendship f " +
            "WHERE f.requester.id = :userId " +
            "AND f.status = 'FRIENDSHIP' " +
            "AND f.receiver.status = 'ACTIVE' " +
            "UNION " +
            "SELECT f.requester FROM Friendship f " +
            "WHERE f.receiver.id = :userId " +
            "AND f.status = 'FRIENDSHIP' " +
            "AND f.requester.status = 'ACTIVE'")
    List<User> findAllActiveFriends(@Param("userId") Long userId);
}