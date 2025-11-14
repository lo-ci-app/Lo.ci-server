package com.teamfiv5.fiv5.repository;

import com.teamfiv5.fiv5.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByProviderIdAndProvider(String providerId, String provider);
    boolean existsByNickname(String nickname);
    List<User> findByBluetoothTokenIn(List<String> tokens);
    Optional<User> findByBluetoothToken(String bluetoothToken);
    boolean existsByBluetoothToken(String bluetoothToken);
}