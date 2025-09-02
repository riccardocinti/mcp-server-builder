package com.riccardocinti.mcp_server_build.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.mcp_server_build.exceptions.BuilderToolDetectionException;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.ProjectInfo;
import com.riccardocinti.mcp_server_build.mcp_server_build.service.interfaces.BuilderToolDetectionService;
import org.springframework.stereotype.Service;

@Service
public class BuilderToolDetectionServiceImpl implements BuilderToolDetectionService {
    @Override
    public BuilderConfiguration detectAndAnalyzeBuildTool(ProjectInfo projectInfo) throws BuilderToolDetectionException {
        return null;
    }
}
