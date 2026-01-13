package com.teamloci.loci.domain.badge;

import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<BadgeResponse> getBadgeList(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Badge> allBadges = badgeRepository.findAll();

        Set<Long> acquiredIds = userBadgeRepository.findByUser(user).stream()
                .map(ub -> ub.getBadge().getId())
                .collect(Collectors.toSet());

        Long mainBadgeId = user.getMainBadge() != null ? user.getMainBadge().getId() : null;
        boolean isKorean = "KR".equalsIgnoreCase(user.getCountryCode());

        return allBadges.stream()
                .map(badge -> {
                    boolean isAcquired = acquiredIds.contains(badge.getId());
                    String name = isAcquired ? (isKorean ? badge.getNameKr() : badge.getNameEn()) : "???";
                    String desc = isAcquired
                            ? (isKorean ? badge.getDescriptionKr() : badge.getDescriptionEn())
                            : (isKorean ? "조건을 달성하여 배지를 획득하세요." : "Achieve goals to unlock.");

                    return BadgeResponse.builder()
                            .id(badge.getId())
                            .name(name)
                            .description(desc)
                            .condition(isKorean ? badge.getConditionKr() : badge.getConditionEn())
                            .imageUrl(isAcquired ? badge.getImageUrl() : badge.getLockedImageUrl())
                            .isAcquired(isAcquired)
                            .isMain(badge.getId().equals(mainBadgeId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void setMainBadge(Long userId, Long badgeId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Badge badge = badgeRepository.findById(badgeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 배지입니다."));

        if (!userBadgeRepository.existsByUserAndBadge(user, badge)) {
            throw new IllegalArgumentException("획득하지 않은 배지입니다.");
        }

        user.updateMainBadge(badge);
    }

    @Transactional
    public void awardBadge(User user, BadgeType type) {
        Badge badge = badgeRepository.findByType(type)
                .orElseThrow(() -> new IllegalStateException("Badge Metadata Missing: " + type));

        if (userBadgeRepository.existsByUserAndBadge(user, badge)) {
            return;
        }

        userBadgeRepository.save(new UserBadge(user, badge));
        log.info("Badge Awarded: User {}, Badge {}", user.getId(), type);

        boolean isKorean = "KR".equalsIgnoreCase(user.getCountryCode());
        String description = isKorean ? badge.getDescriptionKr() : badge.getDescriptionEn();

        notificationService.send(user, NotificationType.BADGE_ACQUIRED, user.getId(), badge.getImageUrl(), description);

        if (user.getMainBadge() == null) {
            user.updateMainBadge(badge);
            userRepository.save(user); 
        }
    }
}