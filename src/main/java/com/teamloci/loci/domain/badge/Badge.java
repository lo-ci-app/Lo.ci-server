package com.teamloci.loci.domain.badge;

import com.teamloci.loci.global.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Badge extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private BadgeType type;

    private String nameKr;
    private String nameEn;

    private String conditionKr;
    private String conditionEn;

    private String descriptionKr;
    private String descriptionEn;

    private String imageUrl;
    private String lockedImageUrl;
}