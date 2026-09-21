package com.brijesh.notify.notifications;


import org.springframework.stereotype.Component;

@Component
public class NotificationComposer {

    public String composeTitle(EnrichedEvent e) {
        return "[%s] Issue #%d %s".formatted(e.repoFullName(), e.issueNumber(), e.action());
    }

    public String composeBody(EnrichedEvent e) {
        StringBuilder body = new StringBuilder(e.title());
        if (e.assignedToSomeone()) body.append(" — assigned");
        if (e.hasOpenPrFix()) body.append(" — already has a linked PR");
        body.append(" (").append(e.reactionCount()).append(" reactions)");
        return body.toString();
    }
}
