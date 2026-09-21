package com.brijesh.notify.notifications;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.brijesh.notify.events.RawEvent;
import com.brijesh.notify.mcp.GithubMcpService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
public class GithubEventEnrichmentService {

    private final GithubMcpService gitHubMcpService;
    private final ObjectMapper objectMapper;

    public EnrichedEvent enrich(RawEvent rawEvent) throws Exception {
        JsonNode payload = objectMapper.readTree(rawEvent.getPayloadJson());
        JsonNode issueNode = payload.path("issue");

        if (issueNode.isMissingNode()) {
            return null;   // not an issue-related webhook (e.g. a push event) — nothing to enrich yet
        }

        String action = payload.path("action").asText(null);
        int issueNumber = issueNode.path("number").asInt();
        String[] ownerRepo = payload.path("repository").path("full_name").asText().split("/", 2);

        JsonNode context = gitHubMcpService.getIssueDetails(
                rawEvent.getUserId(), ownerRepo[0], ownerRepo[1], issueNumber);

        return new EnrichedEvent(
                rawEvent.getUserId(),
                rawEvent.getId(),
                action,
                ownerRepo[0] + "/" + ownerRepo[1],
                issueNumber,
                context.path("title").asText(),
                context.path("state").asText(),
                context.path("assignees").isArray() && context.path("assignees").size() > 0,
                context.path("reactions").path("total_count").asInt(),
                context.path("closed_by_pull_requests").path("total_count").asInt() > 0
        );
    }
}
