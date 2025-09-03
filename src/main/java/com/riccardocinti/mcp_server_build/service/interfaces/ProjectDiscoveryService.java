package com.riccardocinti.mcp_server_build.service.interfaces;

import com.riccardocinti.mcp_server_build.exceptions.ProjectDiscoveryException;
import com.riccardocinti.mcp_server_build.model.ProjectInfo;

public interface ProjectDiscoveryService {

    ProjectInfo discoverAndValidateProject(String projectPath) throws ProjectDiscoveryException;
}
