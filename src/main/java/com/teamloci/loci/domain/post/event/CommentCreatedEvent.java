package com.teamloci.loci.domain.post.event;

import com.teamloci.loci.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommentCreatedEvent {
    private final User user;
}