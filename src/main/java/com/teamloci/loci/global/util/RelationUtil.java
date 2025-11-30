package com.teamloci.loci.global.util;

import com.teamloci.loci.domain.Friendship;
import com.teamloci.loci.domain.FriendshipStatus;

public class RelationUtil {

    private RelationUtil() {}

    public static String resolveStatus(Friendship f, Long myUserId) {
        if (f == null) return "NONE";

        if (f.getStatus() == FriendshipStatus.FRIENDSHIP) return "FRIEND";

        if (f.getRequester().getId().equals(myUserId)) {
            return "PENDING_SENT";
        } else {
            return "PENDING_RECEIVED";
        }
    }
}