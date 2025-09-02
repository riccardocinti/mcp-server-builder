package com.riccardocinti.mcp_server_build.mcp_server_build.model;

import com.riccardocinti.mcp_server_build.mcp_server_build.model.enums.BuildTool;

import java.util.List;

public record ProjectInfo(String projectPath,
                          String projectName,
                          String projectVersion,
                          BuildTool buildTool,
                          String buildFilePath,
                          List<String> additionalFiles) {
}
