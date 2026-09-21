package com.brijesh.notify.notifications;


import com.brijesh.notify.events.RawEvent;
import com.brijesh.notify.events.RawEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationPipelineListener {

    private final RawEventRepository rawEventRepository;
    private final GithubEventEnrichmentService enrichmentService;
    private final PrioritizationEngine prioritizationEngine;
    private final NotificationComposer notificationComposer;
    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Async
    @EventListener
    public void onRawEventSaved(RawEventSavedEvent event) {
        RawEvent rawEvent = rawEventRepository.findById(event.rawEventId()).orElse(null);
        if (rawEvent == null) return;

        try {
            EnrichedEvent enriched = enrichmentService.enrich(rawEvent);
            if (enriched == null) {
                log.info("Skipping raw event {} — nothing to enrich (not an issue event)", rawEvent.getId());
                return;
            }

            Priority priority = prioritizationEngine.score(enriched);

            Notification notification = Notification.builder()
                    .userId(rawEvent.getUserId())
                    .rawEventId(rawEvent.getId())
                    .title(notificationComposer.composeTitle(enriched))
                    .body(notificationComposer.composeBody(enriched))
                    .priority(priority.name())
                    .source(rawEvent.getSource())
                    .build();

            notificationRepository.save(notification);
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(rawEvent.getUserId()),
                    "/queue/notifications",
                    notification
            );
            log.info("Pushed live notification to user {}", rawEvent.getUserId());
            log.info("Created [{}] notification for user {}: {}", priority, rawEvent.getUserId(), notification.getTitle());

        } catch (Exception e) {
            log.error("Failed to process raw event {}", rawEvent.getId(), e);
        }
    }
}
