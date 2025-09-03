package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.exceptions.DependencyResolutionException;
import com.riccardocinti.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.model.DependencyResult;
import com.riccardocinti.mcp_server_build.service.interfaces.DependencyResolutionService;
import org.springframework.stereotype.Service;

@Service
public class DependencyResolutionServiceImpl implements DependencyResolutionService {
    @Override
    public DependencyResult resolveDependencies(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) throws DependencyResolutionException {
        return null;
    }
}
