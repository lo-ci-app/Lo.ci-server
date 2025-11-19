package com.teamloci.loci.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "providerId"}),
        @UniqueConstraint(columnNames = {"nickname"})
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    private String email;

    @Column(nullable = false, unique = true)
    private String nickname;

    @Column(nullable = true)
    private String profileUrl;

    @Column(nullable = true, length = 255)
    private String bio;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "bluetooth_token", unique = true)
    private String bluetoothToken;

    @Column(name = "fcm_token")
    private String fcmToken;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    @Builder
    public User(String email, String nickname, String profileUrl, String provider, String providerId) {
        this.email = email;
        this.nickname = nickname;
        this.profileUrl = profileUrl;
        this.provider = provider;
        this.providerId = providerId;
        this.status = UserStatus.ACTIVE;
        this.bio = null;
    }

    public void updateProfile(String nickname, String bio) {
        this.nickname = nickname;
        this.bio = bio;
    }

    public void updateProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public void updateBluetoothToken(String bluetoothToken) {
        this.bluetoothToken = bluetoothToken;
    }

    public void withdraw() {
        this.email = null;
        this.nickname = "탈퇴한사용자_" + this.id;
        this.profileUrl = null;
        this.bio = null;
        this.providerId = "DELETED_" + this.providerId;
        this.status = UserStatus.DELETED;
        this.bluetoothToken = null;
        this.fcmToken = null;
    }
}