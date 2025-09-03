package com.riccardocinti.mcp_server_build.mcp_server_build.model;

import com.riccardocinti.mcp_server_build.mcp_server_build.model.enums.BuildTool;

public record BuildToolInfo(BuildTool buildTool, String buildFilePath) {
}
