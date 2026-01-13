package com.teamloci.loci.domain.auth.event;

import com.teamloci.loci.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserLoginEvent {
    private final User user;
}