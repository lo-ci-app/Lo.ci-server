package com.teamloci.loci.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_push_logs")
public class DailyPushLog {

    @Id
    private String id;

    @Builder
    public DailyPushLog(Long userId, LocalDate date) {
        this.id = date.toString() + "_" + userId;
    }
}