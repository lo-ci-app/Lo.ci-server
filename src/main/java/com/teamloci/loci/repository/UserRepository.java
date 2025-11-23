package com.teamloci.loci.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.teamloci.loci.domain.User;

import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByHandle(String handle);
    Optional<User> findByHandle(String handle);

    Optional<User> findByPhoneSearchHash(String phoneSearchHash);
    List<User> findByPhoneSearchHashIn(List<String> searchHashes);
    List<User> findByBluetoothTokenIn(List<String> tokens);
    boolean existsByBluetoothToken(String bluetoothToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdWithLock(@Param("userId") Long userId);

    List<User> findTop10ByHandleContainingOrNicknameContaining(String handleKeyword, String nicknameKeyword);
}