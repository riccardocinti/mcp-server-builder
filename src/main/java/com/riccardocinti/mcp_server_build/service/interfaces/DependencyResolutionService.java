package com.riccardocinti.mcp_server_build.service.interfaces;

import com.riccardocinti.mcp_server_build.exceptions.DependencyResolutionException;
import com.riccardocinti.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.model.DependencyResult;

public interface DependencyResolutionService {

    DependencyResult resolveDependencies(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) throws DependencyResolutionException;
}
