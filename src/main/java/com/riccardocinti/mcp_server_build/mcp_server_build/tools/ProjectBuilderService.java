package com.riccardocinti.mcp_server_build.mcp_server_build.tools;

import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuildResult;
import com.riccardocinti.mcp_server_build.mcp_server_build.service.BuilderService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Service
public class ProjectBuilderService {

    private final BuilderService builderService;

    public ProjectBuilderService(BuilderService builderService) {
        this.builderService = builderService;
    }

    @Tool(name = "build_project", description = "Build a valid project based on its configuration")
    public BuildResult buildProject(String projectPath) {
        return builderService.buildProject(projectPath);
    }

}
