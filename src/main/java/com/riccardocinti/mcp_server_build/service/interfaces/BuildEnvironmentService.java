package com.riccardocinti.mcp_server_build.service.interfaces;

import com.riccardocinti.mcp_server_build.exceptions.BuildEnvironmentException;
import com.riccardocinti.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.model.BuilderConfiguration;

public interface BuildEnvironmentService {

    BuildEnvironment prepareBuildEnvironment(BuilderConfiguration builderConfig) throws BuildEnvironmentException;

}
