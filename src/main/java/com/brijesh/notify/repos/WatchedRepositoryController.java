package com.brijesh.notify.repos;


import com.brijesh.notify.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class WatchedRepositoryController {

    private final GithubWebhookAutomationService webhookAutomationService;
    private final CurrentUserService currentUserService;
    private final GithubRepoService githubRepoService;
    private final WatchedRepositoryRepository watchedRepositoryRepository;

    @PostMapping("/watch")
    public void watch(@AuthenticationPrincipal UserDetails userDetails,
                      @RequestParam String owner,
                      @RequestParam String repo) throws Exception {
        Long userId = currentUserService.resolveId(userDetails);
        webhookAutomationService.watchRepository(userId,owner,repo);
    }

    @DeleteMapping("/watch")
    public void unwatch(@AuthenticationPrincipal UserDetails userDetails,
                        @RequestParam String owner,
                        @RequestParam String repo) {
        Long userId = currentUserService.resolveId(userDetails);
        webhookAutomationService.unwatchRepository(userId,owner,repo);
    }



    @GetMapping("/github")
    public List<RepoOption> listGithubRepos(@AuthenticationPrincipal UserDetails userDetails) throws Exception {
        Long userId = currentUserService.resolveId(userDetails);
        List<GithubRepoService.GitHubRepoSummary> repos = githubRepoService.listRepositories(userId);

        Set<String> watchedNames = watchedRepositoryRepository.findByUserId(userId).stream()
                .map(WatchedRepository::getRepoFullName)
                .collect(Collectors.toSet());

        return repos.stream()
                .map(r -> new RepoOption(r.fullName(), r.isPrivate(), r.description(), watchedNames.contains(r.fullName())))
                .toList();
    }

    public record RepoOption(String fullName, boolean isPrivate, String description, boolean watched) {}
}
