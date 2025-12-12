package com.teamloci.loci.domain.notification;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_push_logs", indexes = {
        @Index(name = "idx_push_date", columnList = "date")
})
public class DailyPushLog {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Builder
    public DailyPushLog(Long userId, LocalDate date) {
        this.id = date.toString() + "_" + userId;
        this.userId = userId;
        this.date = date;
    }

    public DailyPushLog(String id, Long userId, LocalDate date) {
        this.id = id;
        this.userId = userId;
        this.date = date;
    }
}