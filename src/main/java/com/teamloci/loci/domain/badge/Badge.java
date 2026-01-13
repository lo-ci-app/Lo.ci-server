package com.teamloci.loci.domain.badge;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Badge {

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