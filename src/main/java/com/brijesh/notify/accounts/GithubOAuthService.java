package com.brijesh.notify.accounts;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubOAuthService {

    private final ConnectedAccountRepository connectedAccountRepository;
    private final OAuthStateService oAuthStateService;
    private final TokenEncryptionService tokenEncryptionService;
    private final RestClient restClient = RestClient.create();

    @Value("${github.oauth.client-id}")
    private String clientId;

    @Value("${github.oauth.client-secret}")
    private String clientSecret;

    @Value("${github.oauth.redirect-uri}")
    private String redirectUri;

    public String buildAuthorizationUrl(Long userId) {
        String state = oAuthStateService.createState(userId);

        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "repo")   // read/write access to repos including private ones
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    public void handleCallback(String code, String state) {
        Long userId = oAuthStateService.parseUserId(state);

        GitHubTokenResponse tokenResponse = restClient.post()
                .uri("https://github.com/login/oauth/access_token")
                .header("Accept", "application/json")
                .body(Map.of(
                        "client_id", clientId,
                        "client_secret", clientSecret,
                        "code", code,
                        "redirect_uri", redirectUri
                ))
                .retrieve()
                .body(GitHubTokenResponse.class);

        if (tokenResponse == null || tokenResponse.accessToken() == null) {
            throw new IllegalStateException("GitHub did not return an access token");
        }

        GitHubUserResponse githubUser = restClient.get()
                .uri("https://api.github.com/user")
                .header("Authorization", "Bearer " + tokenResponse.accessToken())
                .header("Accept", "application/vnd.github+json")
                .retrieve()
                .body(GitHubUserResponse.class);

        String githubUsername = githubUser.login;

        connectedAccountRepository.findByProviderAndExternalUsernameIgnoreCase("GITHUB", githubUsername)
                .ifPresent(existing -> {
                    if (!existing.getUserId().equals(userId)) {
                        throw new IllegalStateException(
                                "This Github account is already connected to a different Notify account"
                        );
                    }
                });

        ConnectedAccount account = connectedAccountRepository
                .findByUserIdAndProvider(userId, "GITHUB")
                .orElse(ConnectedAccount.builder().userId(userId).provider("GITHUB").build());

        account.setAccessTokenEncrypted(tokenEncryptionService.encrypt(tokenResponse.accessToken()));
        account.setExternalUsername(githubUser != null ? githubUser.login() : null);
        account.setScope(tokenResponse.scope());

        connectedAccountRepository.save(account);
        log.info("Connected GitHub account '{}' for user {}", account.getExternalUsername(), userId);
    }

    private record GitHubTokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            String scope) {}

    private record GitHubUserResponse(String login, Long id) {}
}