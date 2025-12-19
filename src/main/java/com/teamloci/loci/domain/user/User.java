package com.teamloci.loci.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"handle"}),
        @UniqueConstraint(columnNames = {"phone_search_hash"})
}, indexes = {
        @Index(name = "idx_is_auto_archive", columnList = "is_auto_archive")
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

    @Column(name = "fcm_token")
    private String fcmToken;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean isAutoArchive = true;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long friendCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long postCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long streakCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private long visitedPlaceCount = 0;

    @Column(nullable = false)
    @ColumnDefault("0")
    private int totalIntimacyLevel = 0;

    private LocalDate lastPostDate;

    @Column(nullable = false)
    private String timezone = "Asia/Seoul";

    @Column(nullable = false)
    private boolean isNewPostPushEnabled = true;

    @Column(nullable = false)
    private boolean isLociTimePushEnabled = true;

    @Column(name = "bluetooth_token", length = 8, unique = true, nullable = false)
    private String bluetoothToken;

    @Builder
    public User(String handle, String nickname, String profileUrl, String phoneEncrypted, String phoneSearchHash, String countryCode) {
        this.handle = handle;
        this.nickname = nickname;
        this.profileUrl = profileUrl;
        this.phoneEncrypted = phoneEncrypted;
        this.phoneSearchHash = phoneSearchHash;
        this.countryCode = countryCode;
        this.status = UserStatus.ACTIVE;
        this.timezone = "Asia/Seoul";
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

    public void updateCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public void updateTimezone(String timezone) {
        this.timezone = timezone;
    }

    public ZoneId getZoneIdOrDefault() {
        try {
            return (this.timezone != null) ? ZoneId.of(this.timezone) : ZoneId.of("Asia/Seoul");
        } catch (DateTimeException e) {
            return ZoneId.of("Asia/Seoul");
        }
    }

    public void increasePostCount() {
        this.postCount++;
    }

    public void increaseVisitedPlaceCount() {
        this.visitedPlaceCount++;
    }

    public void updateStreakInfo(long streakCount, LocalDate lastPostDate) {
        this.streakCount = streakCount;
        this.lastPostDate = lastPostDate;
    }

    public void decreasePostCount() {
        if (this.postCount > 0) {
            this.postCount--;
        }
    }

    public void decreaseVisitedPlaceCount() {
        if (this.visitedPlaceCount > 0) {
            this.visitedPlaceCount--;
        }
    }

    public void updateSettings(Boolean isAutoArchive, Boolean isNewPostPushEnabled, Boolean isLociTimePushEnabled) {
        if (isAutoArchive != null) {
            this.isAutoArchive = isAutoArchive;
        }
        if (isNewPostPushEnabled != null) {
            this.isNewPostPushEnabled = isNewPostPushEnabled;
        }
        if (isLociTimePushEnabled != null) {
            this.isLociTimePushEnabled = isLociTimePushEnabled;
        }
    }

    public void setBluetoothToken(String bluetoothToken) {
        this.bluetoothToken = bluetoothToken;
    }
}