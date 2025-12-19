package com.teamloci.loci.domain.settings;

import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService {

    private final UserRepository userRepository;

    @Transactional
    public SettingsDto.Response updateSettings(Long userId, SettingsDto.UpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateSettings(
                request.getIsAutoArchive(),
                request.getIsNewPostPushEnabled(),
                request.getIsLociTimePushEnabled()
        );

        return SettingsDto.Response.builder()
                .isAutoArchive(user.isAutoArchive())
                .isNewPostPushEnabled(user.isNewPostPushEnabled())
                .isLociTimePushEnabled(user.isLociTimePushEnabled())
                .build();
    }

    public SettingsDto.Response getSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return SettingsDto.Response.builder()
                .isAutoArchive(user.isAutoArchive())
                .isNewPostPushEnabled(user.isNewPostPushEnabled())
                .isLociTimePushEnabled(user.isLociTimePushEnabled())
                .build();
    }
}