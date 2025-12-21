package com.teamloci.loci.domain.user;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByHandle(String handle);
    Optional<User> findByHandle(String handle);

    Optional<User> findByPhoneSearchHash(String phoneSearchHash);
    List<User> findByPhoneSearchHashIn(List<String> searchHashes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdWithLock(@Param("userId") Long userId);

    @Query("SELECT u FROM User u " +
            "WHERE (u.handle LIKE %:handle% OR u.nickname LIKE %:nickname%) " +
            "AND u.status = 'ACTIVE'")
    Slice<User> findByHandleContainingOrNicknameContaining(@Param("handle") String handle, @Param("nickname") String nickname, Pageable pageable);

    @Query("SELECT u FROM User u " +
            "WHERE (u.handle LIKE %:keyword% OR u.nickname LIKE %:keyword%) " +
            "AND u.status = 'ACTIVE' " +
            "AND u.id < :cursor " +
            "ORDER BY u.id DESC")
    List<User> searchByKeywordWithCursor(@Param("keyword") String keyword, @Param("cursor") Long cursor, Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' AND u.fcmToken IS NOT NULL AND u.fcmToken <> ''")
    Slice<User> findActiveUsersWithFcmToken(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.status = 'ACTIVE' " +
            "AND u.fcmToken IS NOT NULL AND u.fcmToken <> '' " +
            "AND u.id NOT IN :excludedIds")
    Slice<User> findActiveUsersWithFcmTokenExcludingIds(@Param("excludedIds") List<Long> excludedIds, Pageable pageable);

    List<User> findByHandleIn(List<String> handles);

    @Modifying
    @Query("UPDATE User u SET u.friendCount = u.friendCount + 1 WHERE u.id = :id")
    void increaseFriendCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.friendCount = u.friendCount - 1 WHERE u.id = :id AND u.friendCount > 0")
    void decreaseFriendCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.postCount = u.postCount + 1 WHERE u.id = :id")
    void increasePostCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.postCount = u.postCount - 1 WHERE u.id = :id AND u.postCount > 0")
    void decreasePostCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.streakCount = :streak, u.lastPostDate = :date WHERE u.id = :id")
    void updateStreak(@Param("id") Long id, @Param("streak") Long streak, @Param("date") LocalDate date);

    @Modifying
    @Query("UPDATE User u SET u.visitedPlaceCount = u.visitedPlaceCount + 1 WHERE u.id = :id")
    void increaseVisitedPlaceCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE User u SET u.totalIntimacyLevel = u.totalIntimacyLevel + :delta WHERE u.id = :id")
    void increaseTotalIntimacy(@Param("id") Long id, @Param("delta") int delta);

    boolean existsByBluetoothToken(String bluetoothToken);

    List<User> findByBluetoothTokenIn(List<String> bluetoothTokens);
}