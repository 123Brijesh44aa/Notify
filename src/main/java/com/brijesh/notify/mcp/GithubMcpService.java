package com.brijesh.notify.mcp;


import com.brijesh.notify.accounts.ConnectedAccount;
import com.brijesh.notify.accounts.ConnectedAccountRepository;
import com.brijesh.notify.accounts.TokenEncryptionService;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GithubMcpService {

    private final ConnectedAccountRepository connectedAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final GithubMcpClientFactory clientFactory;
    private final ObjectMapper objectMapper;

    public JsonNode getIssueDetails(Long userId, String owner, String repo, int issueNumber) {
        ConnectedAccount account = connectedAccountRepository.findByUserIdAndProvider(userId,"GITHUB")
                .orElseThrow(() -> new IllegalStateException("No Github account connected for this user"));

        String accessToken = tokenEncryptionService.decrypt(account.getAccessTokenEncrypted());

        McpSyncClient client = clientFactory.createClient(accessToken);

        try {

            McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
                    .name("issue_read")
                    .arguments(Map.of(
                            "method","get",
                            "owner", owner,
                            "repo", repo,
                            "issue_number", issueNumber
                    ))
                    .build();

            McpSchema.CallToolResult result = client.callTool(request);
            return objectMapper.readTree(extractText(result));
        } catch (Exception e){
            throw new RuntimeException("Failed to parse MCP tool result", e);
        }
        finally {
            client.closeGracefully();  // always stop the container, success or failure
        }
    }


    private String extractText(McpSchema.CallToolResult result){
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("MCP tool returned no text content"));
    }
}
