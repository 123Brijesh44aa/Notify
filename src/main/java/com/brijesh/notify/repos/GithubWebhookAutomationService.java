package com.brijesh.notify.repos;


import com.brijesh.notify.accounts.ConnectedAccount;
import com.brijesh.notify.accounts.ConnectedAccountRepository;
import com.brijesh.notify.accounts.TokenEncryptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubWebhookAutomationService {

    private static final List<String> IMPORTANT_GITHUB_EVENTS = List.of(
            "push","pull_request","pull_request_review","pull_request_review_comment","issues","issue_comment",
            "commit_comment",
            "create",
            "delete",
            "release",
            "workflow_run",
            "workflow_job",
            "check_run",
            "check_suite",
            "status",
            "deployment",
            "deployment_status",
            "repository",
            "star",
            "fork",
            "watch"
    );

    private final ConnectedAccountRepository connectedAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final WatchedRepositoryRepository watchedRepositoryRepository;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    @Value("${app.public-url}")
    private String publicUrl;

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    public void watchRepository(Long userId, String owner, String repo) throws Exception {
        ConnectedAccount account = connectedAccountRepository.findByUserIdAndProvider(userId, "GITHUB")
                .orElseThrow(() -> new IllegalStateException("No GitHub account connected"));

        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());
        String repoFullName = owner + "/" + repo;

        // check GitHub's actual state before creating anything
        String targetUrl = publicUrl + "/api/webhooks/github";
        String existingHooksJson = restClient.get()
                .uri("https://api.github.com/repos/{owner}/{repo}/hooks", owner, repo)
                .header("Authorization","Bearer "+accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(String.class);

        var existingHooks = objectMapper.readTree(existingHooksJson);
        for (var hook : existingHooks) {
            String hookUrl = hook.path("config").path("url").asText("");
            if (hookUrl.equals(targetUrl)){
                // Github already has exactly the hook we'd create - adopt it instead of duplicating
                Long hookId = hook.path("id").asLong();
                reconcileWatchedRepo(userId,repoFullName,hookId);
                log.info("Adopted pre-existing webhook {} for {} (user {})", hookId, repoFullName, userId);
                return;
            }
        }

        String createResponseJson = restClient.post()
                .uri("https://api.github.com/repos/{owner}/{repo}/hooks", owner, repo)
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .body(Map.of(
                        "name", "web",
                        "active", true,
                        "events", IMPORTANT_GITHUB_EVENTS,
                        "config", Map.of(
                                "url", targetUrl,
                                "content_type", "json",
                                "secret", webhookSecret
                        )
                ))
                .retrieve()
                .body(String.class);

        Long newHookId = objectMapper.readTree(createResponseJson).path("id").asLong();
        reconcileWatchedRepo(userId,repoFullName,newHookId);
        log.info("Created new webhook {} for {} (user {})", newHookId,repoFullName, userId);
    }

    private void reconcileWatchedRepo(Long userId, String repoFullName, Long hookId){
        WatchedRepository watched = watchedRepositoryRepository
                .findByUserIdAndRepoFullName(userId,repoFullName)
                .orElse(WatchedRepository.builder().userId(userId).repoFullName(repoFullName).build());
        watched.setGithubWebhookId(hookId);
        watchedRepositoryRepository.save(watched);
    }

    public void unwatchRepository(Long userId, String owner, String repo) {
        String repoFullName = owner + "/" + repo;

        WatchedRepository watched = watchedRepositoryRepository.findByUserIdAndRepoFullName(userId, repoFullName)
                .orElseThrow(() -> new IllegalStateException("This repo isn't being watched"));

        ConnectedAccount account = connectedAccountRepository.findByUserIdAndProvider(userId, "GITHUB")
                .orElseThrow(() -> new IllegalStateException("No GitHub account connected"));
        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());

        restClient.delete()
                .uri("https://api.github.com/repos/{owner}/{repo}/hooks/{hookId}",
                        owner, repo, watched.getGithubWebhookId())
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .toBodilessEntity();

        watchedRepositoryRepository.delete(watched);
        log.info("Removed GitHub webhook for {} (user {})", repoFullName, userId);
    }

    private record GitHubWebhookResponse(Long id) {}

}
