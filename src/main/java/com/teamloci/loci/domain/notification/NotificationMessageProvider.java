package com.teamloci.loci.domain.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.IllegalFormatException;
import java.util.Map;

@Slf4j
@Component
public class NotificationMessageProvider {

    public static final String DEFAULT_LANG = "KR";

    private static final Map<String, Map<NotificationType, String[]>> MESSAGE_TEMPLATES = Map.of(
            "KR", Map.ofEntries(
                    Map.entry(NotificationType.FRIEND_REQUEST, new String[]{"친구가 되고 싶대요! 👋", "%s님이 회원님과 가까워지고 싶어 해요. 확인해 볼까요?"}),
                    Map.entry(NotificationType.FRIEND_ACCEPTED, new String[]{"우리는 이제 친구! 🤝", "%s님과 친구가 되었어요! 인사를 건네보세요."}),
                    Map.entry(NotificationType.FRIEND_VISITED, new String[]{"누가 다녀갔을까요? 👀", "%s님이 회원님의 소식에 관심을 보이고 있어요."}),
                    Map.entry(NotificationType.NEW_POST, new String[]{"놓치지 마세요! 🔥", "%s님의 새로운 일상이 올라왔어요. 지금 구경하러 갈까요?"}),
                    Map.entry(NotificationType.POST_TAGGED, new String[]{"함께한 순간 📸", "%s님이 회원님과 함께한 순간을 기록했어요."}),
                    Map.entry(NotificationType.POST_COMMENT, new String[]{"새로운 댓글 💬", "%s님이 댓글을 남겼습니다."}),
                    Map.entry(NotificationType.COMMENT_MENTION, new String[]{"새로운 언급 📣", "%s님이 회원님을 언급했습니다."}),
                    Map.entry(NotificationType.POST_REACTION, new String[]{"새로운 반응 👀", "%s님이 회원님의 게시물에 반응했습니다."}),
                    Map.entry(NotificationType.COMMENT_LIKE, new String[]{"공감 꾹! 👍", "%s님이 회원님의 댓글에 공감했어요."}),
                    Map.entry(NotificationType.NUDGE, new String[]{"똑똑! 사진이 보고 싶어요 👀", "%s님이 회원님의 새로운 소식을 기다리고 있어요. 사진을 올려보세요!"}),
                    Map.entry(NotificationType.LOCI_TIME, new String[]{"It's Loci Time! ⚡️", "지금 바로 친구들과 시공간을 넘어 연결되어 보세요."}),
                    Map.entry(NotificationType.INTIMACY_LEVEL_UP, new String[]{"우리 더 친해졌어요! 🎉", "%s님과의 친밀도가 Lv.%s이 되었어요! 특별한 사이가 되어가고 있네요."})
            ),

            "US", Map.ofEntries(
                    Map.entry(NotificationType.FRIEND_REQUEST, new String[]{"New Friend Request! 👋", "%s wants to be friends with you. Check it out!"}),
                    Map.entry(NotificationType.FRIEND_ACCEPTED, new String[]{"You are now friends! 🤝", "You became friends with %s! Say hello."}),
                    Map.entry(NotificationType.FRIEND_VISITED, new String[]{"Guess who visited? 👀", "%s is interested in your updates."}),
                    Map.entry(NotificationType.NEW_POST, new String[]{"Don't miss it! 🔥", "%s shared a new moment. Let's go see it!"}),
                    Map.entry(NotificationType.POST_TAGGED, new String[]{"Moments Together 📸", "%s tagged you in a moment you shared together."}),
                    Map.entry(NotificationType.POST_COMMENT, new String[]{"New Comment 💬", "%s left a comment."}),
                    Map.entry(NotificationType.COMMENT_MENTION, new String[]{"New Mention 📣", "%s mentioned you."}),
                    Map.entry(NotificationType.POST_REACTION, new String[]{"New Reaction 👀", "%s reacted to your post."}),
                    Map.entry(NotificationType.COMMENT_LIKE, new String[]{"Thumbs up! 👍", "%s liked your comment."}),
                    Map.entry(NotificationType.NUDGE, new String[]{"Knock knock! Miss you 👀", "%s is waiting for your update. Share a photo!"}),
                    Map.entry(NotificationType.LOCI_TIME, new String[]{"It's Loci Time! ⚡️", "Connect with your friends across time and space right now."}),
                    Map.entry(NotificationType.INTIMACY_LEVEL_UP, new String[]{"Level Up! 🎉", "Intimacy with %s reached Lv.%s! You're getting closer."})
            )
    );

    public NotificationContent getMessage(NotificationType type, String countryCode, Object... args) {
        String lang = StringUtils.hasText(countryCode) ? countryCode.toUpperCase() : DEFAULT_LANG;

        if (type == NotificationType.NUDGE && args != null && args.length == 2) {
            return getCustomNudgeMessage(lang, args);
        }

        Map<NotificationType, String[]> langMap = MESSAGE_TEMPLATES.getOrDefault(lang, MESSAGE_TEMPLATES.get(DEFAULT_LANG));
        String[] templates = langMap.get(type);

        if (templates == null) {
            templates = MESSAGE_TEMPLATES.get(DEFAULT_LANG).get(type);
            if (templates == null) {
                return new NotificationContent("Loci 알림", "새로운 소식이 있습니다.");
            }
        }

        String title = templates[0];
        String body = templates[1];

        if (args != null && args.length > 0) {
            try {
                if (title.contains("%s")) {
                    title = String.format(title, args);
                }
                if (body.contains("%s")) {
                    body = String.format(body, args);
                }
            } catch (IllegalFormatException e) {
                log.warn("알림 메시지 포맷팅 실패. type={}, args={}, error={}", type, args, e.getMessage());
            }
        }

        return new NotificationContent(title, body);
    }

    private NotificationContent getCustomNudgeMessage(String lang, Object[] args) {
        String nickname = String.valueOf(args[0]);
        String customMessage = String.valueOf(args[1]);

        String titleTemplate = "KR".equals(lang) ? "%s님의 콕 찌르기" : "%s's Nudge";
        String title = String.format(titleTemplate, nickname);

        return new NotificationContent(title, customMessage);
    }

    public record NotificationContent(String title, String body) {}
}