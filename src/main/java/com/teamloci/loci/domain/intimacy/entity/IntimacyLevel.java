package com.teamloci.loci.domain.intimacy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "intimacy_levels")
public class IntimacyLevel {

    @Id
    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Integer requiredTotalScore;

    public IntimacyLevel(Integer level, Integer requiredTotalScore) {
        this.level = level;
        this.requiredTotalScore = requiredTotalScore;
    }
}