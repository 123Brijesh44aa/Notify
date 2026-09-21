package com.brijesh.notify.mcp;


import com.brijesh.notify.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class McpTestController {

    private final GithubMcpService githubMcpService;
    private final CurrentUserService currentUserService;

    @GetMapping("/api/test/github-issue")
    public String testIssue(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam String owner,
                            @RequestParam String repo,
                            @RequestParam int issueNumber) {
        Long userId = currentUserService.resolveId(userDetails);
        return githubMcpService.getIssueDetails(userId,owner, repo, issueNumber).toString();
    }
}
