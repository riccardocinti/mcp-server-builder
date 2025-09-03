package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.model.*;
import com.riccardocinti.mcp_server_build.model.enums.ErrorType;
import com.riccardocinti.mcp_server_build.service.interfaces.BuildResultService;
import org.springframework.stereotype.Service;

@Service
public class BuildResultServiceImpl implements BuildResultService {
    @Override
    public BuildResult compileBuildResult(ProjectInfo projectInfo, BuilderConfiguration builderConfig, BuildEnvironment buildEnv, DependencyResult depResult, CompilationResult compResult, ArtifactInfo artifactInfo) {
        return null;
    }

    @Override
    public BuildResult createFailureResult(ErrorType errorType, String message, String projectPath) {
        return null;
    }
}
