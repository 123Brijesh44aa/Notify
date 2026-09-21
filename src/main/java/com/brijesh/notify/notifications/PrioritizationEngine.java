package com.brijesh.notify.notifications;


import org.springframework.stereotype.Component;

@Component
public class PrioritizationEngine {

    public Priority score(EnrichedEvent e){
        int score = 0;

        if ("assigned".equals(e.action())) score += 40;
        if (e.assignedToSomeone()) score += 20;
        if ("opened".equals(e.action())) score += 20;
        if ("closed".equals(e.action())) score -= 50;
        if (e.hasOpenPrFix()) score -= 15;          // already being handled — lower urgency
        score += Math.min(e.reactionCount() * 2, 20); // popularity signal, capped so it can't dominate

        if (score >= 60) return Priority.CRITICAL;
        if (score >= 40) return Priority.HIGH;
        if (score >= 20) return Priority.MEDIUM;
        return Priority.LOW;
    }
}
