package com.riccardocinti.mcp_server_build.mcp_server_build.service.interfaces;

import com.riccardocinti.mcp_server_build.mcp_server_build.exceptions.BuilderToolDetectionException;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.ProjectInfo;

public interface BuilderToolDetectionService {

    BuilderConfiguration detectAndAnalyzeBuildTool(ProjectInfo projectInfo) throws BuilderToolDetectionException;

}
