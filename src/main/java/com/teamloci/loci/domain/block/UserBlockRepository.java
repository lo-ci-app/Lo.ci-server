package com.teamloci.loci.domain.block;

import com.teamloci.loci.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);

    Optional<UserBlock> findByBlockerAndBlocked(User blocker, User blocked);

    @Query("SELECT ub.blocked.id FROM UserBlock ub WHERE ub.blocker.id = :blockerId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("blockerId") Long blockerId);
}