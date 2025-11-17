package com.teamfiv5.fiv5.repository;

import com.teamfiv5.fiv5.domain.Friendship;
import com.teamfiv5.fiv5.domain.FriendshipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f " +
            "WHERE (f.requester.id = :userId OR f.receiver.id = :userId)")
    List<Friendship> findAllFriendshipsByUserId(@Param("userId") Long userId);

    Optional<Friendship> findByRequesterIdAndReceiverIdAndStatus(
            Long requesterId,
            Long receiverId,
            FriendshipStatus status
    );

    @Query("SELECT f FROM Friendship f JOIN FETCH f.requester " +
            "WHERE f.receiver.id = :receiverId AND f.status = :status")
    List<Friendship> findByReceiverIdAndStatusWithRequester(
            @Param("receiverId") Long receiverId,
            @Param("status") FriendshipStatus status
    );

    @Query("SELECT f FROM Friendship f JOIN FETCH f.receiver " +
            "WHERE f.requester.id = :requesterId AND f.status = :status")
    List<Friendship> findByRequesterIdAndStatusWithReceiver(
            @Param("requesterId") Long requesterId,
            @Param("status") FriendshipStatus status
    );

    @Query("SELECT f FROM Friendship f " +
            "JOIN FETCH f.requester " +
            "JOIN FETCH f.receiver " +
            "WHERE (f.requester.id = :userId OR f.receiver.id = :userId) " +
            "AND f.status = :status")
    List<Friendship> findAllFriendsWithUsers(
            @Param("userId") Long userId,
            @Param("status") FriendshipStatus status
    );

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END " +
            "FROM Friendship f " +
            "WHERE (f.requester.id = :userA AND f.receiver.id = :userB) " +
            "OR (f.requester.id = :userB AND f.receiver.id = :userA)")
    boolean existsFriendshipBetween(
            @Param("userA") Long userA,
            @Param("userB") Long userB
    );

    @Query("SELECT COUNT(f) FROM Friendship f " +
            "WHERE (f.requester.id = :userId OR f.receiver.id = :userId) " +
            "AND f.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") FriendshipStatus status);

    @Query("SELECT f FROM Friendship f " +
            "WHERE ((f.requester.id = :userA AND f.receiver.id = :userB) OR (f.requester.id = :userB AND f.receiver.id = :userA)) " +
            "AND f.status = :status")
    Optional<Friendship> findFriendshipBetweenUsersByStatus(
            @Param("userA") Long userA,
            @Param("userB") Long userB,
            @Param("status") FriendshipStatus status
    );
}