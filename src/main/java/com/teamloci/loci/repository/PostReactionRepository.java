package com.teamloci.loci.repository;

import com.teamloci.loci.domain.PostReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostIdAndUserId(Long postId, Long userId);
}