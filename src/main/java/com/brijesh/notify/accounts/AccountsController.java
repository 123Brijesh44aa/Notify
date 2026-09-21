package com.brijesh.notify.accounts;

import com.brijesh.notify.repos.GithubWebhookAutomationService;
import com.brijesh.notify.repos.WatchedRepository;
import com.brijesh.notify.repos.WatchedRepositoryRepository;
import com.brijesh.notify.user.CurrentUserService;
import com.brijesh.notify.user.User;
import com.brijesh.notify.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountsController {

    private final GithubOAuthService gitHubOAuthService;
    private final ConnectedAccountRepository connectedAccountRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final WatchedRepositoryRepository watchedRepositoryRepository;
    private final GithubWebhookAutomationService webhookAutomationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/github/connect")
    public Map<String, String> connectGithub(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserService.resolveId(userDetails);
        return Map.of("authorizationUrl", gitHubOAuthService.buildAuthorizationUrl(userId));
    }

    @GetMapping("/github/callback")
    public ResponseEntity<Void> githubCallback(@RequestParam String code, @RequestParam String state) {
        gitHubOAuthService.handleCallback(code, state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(frontendUrl + "/settings?connected=github"))
                .build();
    }

    @GetMapping
    public List<ConnectedAccountResponse> listConnectedAccounts(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserId(userDetails);
        return connectedAccountRepository.findByUserId(userId).stream()
                .map(a -> new ConnectedAccountResponse(a.getProvider(), a.getExternalUsername(), a.getConnectedAt()))
                .toList();
    }

    @DeleteMapping("/github")
    public void disconnectGithub(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = currentUserService.resolveId(userDetails);

        for (WatchedRepository w : watchedRepositoryRepository.findByUserId(userId)) {
            String[] parts = w.getRepoFullName().split("/", 2);
            webhookAutomationService.unwatchRepository(userId, parts[0], parts[1]);
        }

        connectedAccountRepository.findByUserIdAndProvider(userId, "GITHUB")
                .ifPresent(connectedAccountRepository::delete);
    }

    private Long currentUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        return user.getId();
    }

    public record ConnectedAccountResponse(String provider, String externalUsername, LocalDateTime connectedAt) {}
}