package com.teamloci.loci.domain.friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserContactRepository extends JpaRepository<UserContact, Long> {
    List<UserContact> findByUserId(Long userId);

    @Query("SELECT uc.phoneSearchHash FROM UserContact uc WHERE uc.user.id = :userId AND uc.phoneSearchHash IS NOT NULL")
    List<String> findPhoneSearchHashesByUserId(@Param("userId") Long userId);
}