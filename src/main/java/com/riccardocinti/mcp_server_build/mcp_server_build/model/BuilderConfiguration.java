package com.riccardocinti.mcp_server_build.mcp_server_build.model;

import com.riccardocinti.mcp_server_build.mcp_server_build.model.enums.BuildTool;

import java.util.List;
import java.util.Map;

public record BuilderConfiguration(
        BuildTool buildTool,
        String javaVersion,
        String mainClass,
        String port,
        List<String> buildCommands,
        Map<String, String> properties,
        DockerHints dockerHints
) {
}
