package com.teamfiv5.fiv5.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class) // 2. JPA Auditing 활성화
public abstract class BaseTimeEntity {

    @CreatedDate // 3. 엔티티 생성 시 자동 저장
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate // 4. 엔티티 수정 시 자동 저장
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}