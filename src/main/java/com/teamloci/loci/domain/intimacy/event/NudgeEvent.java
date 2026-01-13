package com.teamloci.loci.domain.intimacy.event;

import com.teamloci.loci.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NudgeEvent {
    private final User sender;
    private final User receiver;
}