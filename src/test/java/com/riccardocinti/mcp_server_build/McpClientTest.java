package com.riccardocinti.mcp_server_build;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;

public class McpClientTest {

    public static void main(String[] args) {
        McpSyncClient client = null;
        try {
            var stdioParams = ServerParameters.builder("java")
                    .args("-jar", "target/mcp-server-builder-0.0.1-SNAPSHOT.jar")
                    .build();

            var transport = new StdioClientTransport(stdioParams);
            client = McpClient.sync(transport).build();

            // Wait for initialization to complete
            client.initialize();

            // List and demonstrate tools
            var toolsList = client.listTools();
            System.out.println("Available Tools = " + toolsList);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.closeGracefully();
        }
    }
}