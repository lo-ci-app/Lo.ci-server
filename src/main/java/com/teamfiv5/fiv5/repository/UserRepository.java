package com.teamfiv5.fiv5.repository;

import com.teamfiv5.fiv5.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // 제공자(apple)와 제공자 고유 ID로 사용자를 찾기
    Optional<User> findByProviderIdAndProvider(String providerId, String provider);
    
    boolean existsByNickname(String nickname);
    boolean existsByEmail(String email);
}