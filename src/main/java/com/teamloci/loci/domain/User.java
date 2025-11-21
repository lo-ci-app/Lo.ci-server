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
        @UniqueConstraint(columnNames = {"handle"}),
        @UniqueConstraint(columnNames = {"phone_search_hash"})
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String handle;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = true)
    private String profileUrl;

    @Column(name = "phone_encrypted")
    private String phoneEncrypted;

    @Column(name = "phone_search_hash", unique = true, nullable = false)
    private String phoneSearchHash;

    @Column(name = "country_code")
    private String countryCode;

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

    @Builder
    public User(String handle, String nickname, String profileUrl, String phoneEncrypted, String phoneSearchHash, String countryCode) {
        this.handle = handle;
        this.nickname = nickname;
        this.profileUrl = profileUrl;
        this.phoneEncrypted = phoneEncrypted;
        this.phoneSearchHash = phoneSearchHash;
        this.countryCode = countryCode;
        this.status = UserStatus.ACTIVE;
    }

    public void updateProfile(String handle, String nickname) {
        this.handle = handle;
        this.nickname = nickname;
    }

    public void updateProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public void updateBluetoothToken(String bluetoothToken) {
        this.bluetoothToken = bluetoothToken;
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void withdraw() {
        this.handle = "deleted_" + this.id;
        this.nickname = "알수없음";
        this.profileUrl = null;
        this.phoneEncrypted = null;
        this.phoneSearchHash = "deleted_" + this.id;
        this.status = UserStatus.DELETED;
        this.bluetoothToken = null;
        this.fcmToken = null;
    }
}