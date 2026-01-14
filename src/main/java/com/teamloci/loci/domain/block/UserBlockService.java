package com.teamloci.loci.domain.block;

import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserBlockService {

    private final UserBlockRepository userBlockRepository;
    private final UserRepository userRepository;

    @Transactional
    public boolean toggleBlock(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("자기 자신은 차단할 수 없습니다.");
        }

        User blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        User blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return userBlockRepository.findByBlockerAndBlocked(blocker, blocked)
                .map(userBlock -> {
                    userBlockRepository.delete(userBlock);
                    return false;
                })
                .orElseGet(() -> {
                    userBlockRepository.save(UserBlock.builder()
                            .blocker(blocker)
                            .blocked(blocked)
                            .build());
                    return true;
                });
    }

    public List<Long> getBlockedUserIds(Long blockerId) {
        return userBlockRepository.findBlockedUserIdsByBlockerId(blockerId);
    }
}