package com.teamloci.loci.repository;

import com.teamloci.loci.domain.UserContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserContactRepository extends JpaRepository<UserContact, Long> {
    @Modifying
    @Query("DELETE FROM UserContact uc WHERE uc.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    List<UserContact> findByUserId(Long userId);
}