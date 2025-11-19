package com.teamloci.loci.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private Boolean isNewUser;
    private String firebaseCustomToken;
}