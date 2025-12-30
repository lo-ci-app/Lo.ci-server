package com.teamloci.loci.domain.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        boolean isNewUser
) {}