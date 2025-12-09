package com.teamloci.loci.domain.post.event;

import com.teamloci.loci.domain.post.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostCreatedEvent {
    private Post post;
}