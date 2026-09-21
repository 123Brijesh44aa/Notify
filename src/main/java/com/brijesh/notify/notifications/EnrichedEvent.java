package com.brijesh.notify.notifications;

public record EnrichedEvent(
        Long userId,
        Long rawEventId,
        String action,          // "opened", "assigned", "closed", etc. from the webhook
        String repoFullName,
        int issueNumber,
        String title,
        String state,
        boolean assignedToSomeone,
        int reactionCount,
        boolean hasOpenPrFix     // already being resolved by a linked, open PR
) {}