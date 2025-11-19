package com.teamloci.loci.service;

import com.teamloci.loci.domain.GuestbookEntry;
import com.teamloci.loci.domain.User;
import com.teamloci.loci.dto.GuestbookDto;
import com.teamloci.loci.global.exception.CustomException;
import com.teamloci.loci.global.exception.code.ErrorCode;
import com.teamloci.loci.repository.GuestbookEntryRepository;
import com.teamloci.loci.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuestbookService {

    private final GuestbookEntryRepository guestbookEntryRepository;
    private final UserRepository userRepository;

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public GuestbookDto.GuestbookResponse createEntry(Long authorId, Long hostId, GuestbookDto.GuestbookCreateRequest request) {
        if (authorId.equals(hostId)) {
            throw new CustomException(ErrorCode.SELF_GUESTBOOK_ENTRY);
        }

        User author = findUserById(authorId);
        User host = findUserById(hostId);

        GuestbookEntry entry = GuestbookEntry.builder()
                .host(host)
                .author(author)
                .contents(request.getContents())
                .build();

        GuestbookEntry savedEntry = guestbookEntryRepository.save(entry);

        return GuestbookDto.GuestbookResponse.from(savedEntry);
    }

    public List<GuestbookDto.GuestbookResponse> getGuestbook(Long hostId) {
        List<GuestbookEntry> entries = guestbookEntryRepository.findByHostIdWithAuthor(hostId);

        return entries.stream()
                .map(GuestbookDto.GuestbookResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteEntry(Long currentUserId, Long entryId) {
        GuestbookEntry entry = guestbookEntryRepository.findById(entryId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUESTBOOK_ENTRY_NOT_FOUND));

        Long authorId = entry.getAuthor().getId();
        Long hostId = entry.getHost().getId();

        if (!currentUserId.equals(authorId) && !currentUserId.equals(hostId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        guestbookEntryRepository.delete(entry);
    }
}