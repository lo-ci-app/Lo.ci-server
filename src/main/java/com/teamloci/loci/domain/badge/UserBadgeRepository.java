package com.teamloci.loci.domain.badge;

import com.teamloci.loci.domain.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    @EntityGraph(attributePaths = {"badge"})
    List<UserBadge> findByUser(User user);

    boolean existsByUserAndBadge(User user, Badge badge);

    int countByUser(User user);

    void deleteByUser(User user);
}