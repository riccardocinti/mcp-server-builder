package com.riccardocinti.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.exceptions.BuildEnvironmentException;
import com.riccardocinti.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.service.interfaces.BuildEnvironmentService;
import org.springframework.stereotype.Service;

@Service
public class BuildEnvironmentServiceImpl implements BuildEnvironmentService {
    @Override
    public BuildEnvironment prepareBuildEnvironment(BuilderConfiguration builderConfig) throws BuildEnvironmentException {
        return null;
    }
}
