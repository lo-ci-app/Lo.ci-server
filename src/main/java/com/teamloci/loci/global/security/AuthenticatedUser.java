package com.teamloci.loci.global.security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@Getter
@RequiredArgsConstructor
public class AuthenticatedUser {
    private final Long userId;
    private final String nickname;
    private final Collection<? extends GrantedAuthority> authorities;
}