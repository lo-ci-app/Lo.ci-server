package com.teamloci.loci.domain.intimacy.listener;

import com.teamloci.loci.domain.intimacy.event.IntimacyLevelUpEvent;
import com.teamloci.loci.domain.notification.NotificationService;
import com.teamloci.loci.domain.notification.NotificationType;
import com.teamloci.loci.domain.user.User;
import com.teamloci.loci.domain.user.UserRepository;
import com.teamloci.loci.global.error.CustomException;
import com.teamloci.loci.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class IntimacyEventListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Async
    @EventListener
    @Transactional
    public void handleIntimacyLevelUp(IntimacyLevelUpEvent event) {
        try {
            User actor = userRepository.findById(event.getActorId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            User target = userRepository.findById(event.getTargetId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            notificationService.send(
                    target,
                    NotificationType.INTIMACY_LEVEL_UP,
                    "친밀도 레벨 UP! 🔥",
                    actor.getNickname() + "님과의 친밀도가 " + event.getNewLevel() + "레벨이 되었어요!",
                    event.getActorId()
            );
        } catch (Exception e) {
            log.error("친밀도 레벨업 알림 발송 실패: actorId={}, targetId={}", event.getActorId(), event.getTargetId(), e);
        }
    }
}