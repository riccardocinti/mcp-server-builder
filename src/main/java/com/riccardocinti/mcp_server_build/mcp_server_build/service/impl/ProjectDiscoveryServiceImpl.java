package com.riccardocinti.mcp_server_build.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.mcp_server_build.exceptions.ProjectDiscoveryException;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.ProjectInfo;
import com.riccardocinti.mcp_server_build.mcp_server_build.service.interfaces.ProjectDiscoveryService;
import org.springframework.stereotype.Service;

@Service
public class ProjectDiscoveryServiceImpl implements ProjectDiscoveryService {
    @Override
    public ProjectInfo discoverAndValidateProject(String projectPath) throws ProjectDiscoveryException {
        return null;
    }
}
