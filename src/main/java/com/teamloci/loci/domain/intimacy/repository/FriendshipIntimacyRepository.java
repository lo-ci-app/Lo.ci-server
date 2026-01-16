package com.teamloci.loci.domain.intimacy.repository;

import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import com.teamloci.loci.domain.user.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipIntimacyRepository extends JpaRepository<FriendshipIntimacy, Long> {

    Optional<FriendshipIntimacy> findByUserAIdAndUserBId(Long userAId, Long userBId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fi FROM FriendshipIntimacy fi WHERE fi.userAId = :userAId AND fi.userBId = :userBId")
    Optional<FriendshipIntimacy> findByUserAIdAndUserBIdWithLock(@Param("userAId") Long userAId, @Param("userBId") Long userBId);

    @Query("SELECT COALESCE(SUM(fi.level), 0) FROM FriendshipIntimacy fi WHERE fi.userAId = :userId OR fi.userBId = :userId")
    Integer sumLevelByUserId(@Param("userId") Long userId);

    @Query("SELECT fi FROM FriendshipIntimacy fi WHERE fi.userAId = :userId OR fi.userBId = :userId")
    List<FriendshipIntimacy> findAllByUserId(@Param("userId") Long userId);

    @Query(value = """
        SELECT user_id AS userId, SUM(lvl) AS totalLevel
        FROM (
            SELECT user_id_a AS user_id, level AS lvl FROM friendship_intimacies WHERE user_id_a IN :userIds
            UNION ALL
            SELECT user_id_b AS user_id, level AS lvl FROM friendship_intimacies WHERE user_id_b IN :userIds
        ) AS all_levels
        GROUP BY user_id
    """, nativeQuery = true)
    List<UserLevelSum> sumLevelsByUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT fi FROM FriendshipIntimacy fi " +
            "WHERE (fi.userAId = :myUserId AND fi.userBId IN :targetUserIds) " +
            "OR (fi.userBId = :myUserId AND fi.userAId IN :targetUserIds)")
    List<FriendshipIntimacy> findByUserIdAndTargetIdsIn(
            @Param("myUserId") Long myUserId,
            @Param("targetUserIds") List<Long> targetUserIds
    );

    void deleteByUserAIdOrUserBId(Long userAId, Long userBId);
}