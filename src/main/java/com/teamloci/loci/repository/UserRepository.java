package com.teamloci.loci.repository;

import com.teamloci.loci.domain.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderIdAndProvider(String providerId, String provider);
    boolean existsByNickname(String nickname);
    List<User> findByBluetoothTokenIn(List<String> tokens);
    Optional<User> findByBluetoothToken(String bluetoothToken);
    boolean existsByBluetoothToken(String bluetoothToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdWithLock(@Param("userId") Long userId);
}