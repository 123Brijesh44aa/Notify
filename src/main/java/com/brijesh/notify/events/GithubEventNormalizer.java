package com.brijesh.notify.events;


import com.brijesh.notify.accounts.ConnectedAccountRepository;
import com.brijesh.notify.notifications.RawEventSavedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class GithubEventNormalizer {

    private final ConnectedAccountRepository connectedAccountRepository;
    private final RawEventRepository rawEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    public void handle(String eventType, String rawPayload) {
        try {
            JsonNode root = objectMapper.readTree(rawPayload);
            String ownerLogin = root.path("repository").path("owner").path("login").asText(null);

            if (ownerLogin == null) {
                log.warn("Webhook payload had no repository.owner.login, skipping");
                return;
            }

            connectedAccountRepository.findByProviderAndExternalUsernameIgnoreCase("GITHUB", ownerLogin)
                    .ifPresentOrElse(
                            account -> save(account.getUserId(), eventType, root, rawPayload),
                            () -> log.warn("No connected user found for GitHub owner '{}' — dropping event", ownerLogin)
                    );

        } catch (Exception e) {
            log.error("Failed to normalize GitHub webhook payload", e);
        }
    }

    private void save(Long userId, String eventType, JsonNode root, String rawPayload) {
        RawEvent event = RawEvent.builder()
                .userId(userId)
                .source("GITHUB")
                .eventType(eventType)
                .externalId(extractExternalId(root))
                .payloadJson(rawPayload)
                .build();

        rawEventRepository.save(event);
        eventPublisher.publishEvent(new RawEventSavedEvent(event.getId()));
        log.info("Saved raw event: user={}, type={}, externalId={}", userId, eventType, event.getExternalId());
    }

    private String extractExternalId(JsonNode root) {
        if (root.has("issue")) return "issue-" + root.path("issue").path("id").asText();
        if (root.has("pull_request")) return "pr-" + root.path("pull_request").path("id").asText();
        return "event-" + System.currentTimeMillis();
    }
}
