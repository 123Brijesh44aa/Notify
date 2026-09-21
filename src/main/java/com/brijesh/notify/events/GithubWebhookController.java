package com.brijesh.notify.events;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class GithubWebhookController {

    private final GithubWebhookVerifier githubWebhookVerifier;
    private final GithubEventNormalizer eventNormalizer;

    @PostMapping("/github")
    public ResponseEntity<Void> handleGithubWebhook(
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestHeader("X-Github-Event") String eventType,
            @RequestBody String rawPayload
    ) {
        if (!githubWebhookVerifier.isValid(rawPayload,signature)) {
            log.warn("Rejected Github webhook: signature verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        eventNormalizer.handle(eventType, rawPayload);
        return ResponseEntity.ok().build();
    }
}
