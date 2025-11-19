package com.teamloci.loci.dto;

import com.teamloci.loci.domain.GuestbookEntry;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class GuestbookDto {

    @Getter
    @NoArgsConstructor
    public static class GuestbookCreateRequest {
        @NotEmpty(message = "방명록 내용을 입력해주세요.")
        private String contents;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GuestbookResponse {
        private Long id;
        private String contents;
        private PostDto.UserSimpleResponse author;
        private LocalDateTime createdAt;

        public static GuestbookResponse from(GuestbookEntry entry) {
            return GuestbookResponse.builder()
                    .id(entry.getId())
                    .contents(entry.getContents())
                    .author(PostDto.UserSimpleResponse.from(entry.getAuthor()))
                    .createdAt(entry.getCreatedAt())
                    .build();
        }
    }
}