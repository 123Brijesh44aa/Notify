package com.brijesh.notify.repos;


import com.brijesh.notify.accounts.ConnectedAccount;
import com.brijesh.notify.accounts.ConnectedAccountRepository;
import com.brijesh.notify.accounts.TokenEncryptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GithubRepoService {

    private final ConnectedAccountRepository connectedAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    public List<GitHubRepoSummary> listRepositories(Long userId) throws Exception {
        ConnectedAccount account = connectedAccountRepository.findByUserIdAndProvider(userId, "GITHUB")
                .orElseThrow(() -> new IllegalStateException("No GitHub account connected"));

        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());

        List<GitHubRepoSummary> result = new ArrayList<>();
        int page = 1;

        while (true) {
            String rawJson = restClient.get()
                    .uri("https://api.github.com/user/repos?per_page=100&page={page}&affiliation=owner&sort=updated",page)
                    .header("Authorization","Bearer "+accessToken)
                    .header("Accept","application/vnd.github+json")
                    .retrieve()
                    .body(String.class);

            JsonNode repos = objectMapper.readTree(rawJson);
            if (!repos.isArray() || repos.isEmpty()) break;

            for (JsonNode repo: repos) {
                result.add(new GitHubRepoSummary(
                        repo.path("full_name").asText(),
                        repo.path("private").asBoolean(),
                        repo.path("description").asText(null)
                ));
            }

            if (repos.size() < 100) break; // last page - Github returned fewer than a full page
            page++;
        }

        return result;
    }

    public record GitHubRepoSummary(String fullName, boolean isPrivate, String description) {}
}
