package com.riccardocinti.mcp_server_build.mcp_server_build;

import com.riccardocinti.mcp_server_build.mcp_server_build.tools.ProjectBuilderService;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class McpServerBuilderApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerBuilderApplication.class, args);
    }

    @Bean
    public List<ToolCallback> githubTools(ProjectBuilderService projectBuilderService) {
        return List.of(ToolCallbacks.from(projectBuilderService));
    }
}
