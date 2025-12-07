package com.teamloci.loci.domain.intimacy.repository;

import com.teamloci.loci.domain.intimacy.entity.FriendshipIntimacy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FriendshipIntimacyRepository extends JpaRepository<FriendshipIntimacy, Long> {
    Optional<FriendshipIntimacy> findByUserAIdAndUserBId(Long userAId, Long userBId);
}