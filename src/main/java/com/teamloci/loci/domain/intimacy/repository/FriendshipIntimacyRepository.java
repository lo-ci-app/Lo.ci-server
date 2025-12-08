package com.teamloci.loci.domain.intimacy.repository;

import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipIntimacyRepository extends JpaRepository<FriendshipIntimacy, Long> {
    Optional<FriendshipIntimacy> findByUserAIdAndUserBId(Long userAId, Long userBId);

    @Query("SELECT COALESCE(SUM(fi.level), 0) FROM FriendshipIntimacy fi " +
            "WHERE fi.userAId = :userId OR fi.userBId = :userId")
    Integer sumLevelByUserId(@Param("userId") Long userId);

    @Query("SELECT fi FROM FriendshipIntimacy fi WHERE fi.userAId = :userId OR fi.userBId = :userId")
    List<FriendshipIntimacy> findAllByUserId(@Param("userId") Long userId);
}