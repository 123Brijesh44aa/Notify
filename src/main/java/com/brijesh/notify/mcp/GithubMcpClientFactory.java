package com.brijesh.notify.mcp;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Component
public class GithubMcpClientFactory {

    public McpSyncClient createClient(String githubAccessToken) {
        ServerParameters params = ServerParameters.builder("docker")
                .args("run", "-i", "--rm","-e","GITHUB_PERSONAL_ACCESS_TOKEN="+githubAccessToken,"ghcr.io/github/github-mcp-server")
                .env(Map.of("GITHUB_PERSONAL_ACCESS_TOKEN", githubAccessToken))
                .build();

        JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new ObjectMapper());
        StdioClientTransport transport = new StdioClientTransport(params,jsonMapper);

        McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(30))
                .build();

        client.initialize(); // the MCP handshake - must run before listTools()/callTool()

        return client;
    }
}
