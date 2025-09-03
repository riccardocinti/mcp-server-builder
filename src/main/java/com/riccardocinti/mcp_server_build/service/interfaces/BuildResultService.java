package com.riccardocinti.mcp_server_build.service.interfaces;

import com.riccardocinti.mcp_server_build.model.*;
import com.riccardocinti.mcp_server_build.model.enums.ErrorType;

public interface BuildResultService {

    BuildResult compileBuildResult(ProjectInfo projectInfo,
                                   BuilderConfiguration builderConfig,
                                   BuildEnvironment buildEnv,
                                   DependencyResult depResult,
                                   CompilationResult compResult,
                                   ArtifactInfo artifactInfo);

    BuildResult createFailureResult(ErrorType errorType, String message, String projectPath);

}
