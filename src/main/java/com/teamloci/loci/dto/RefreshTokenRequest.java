package com.teamloci.loci.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor // JSON 파싱을 위해 기본 생성자 추가
public class RefreshTokenRequest {
    private String refreshToken;
}