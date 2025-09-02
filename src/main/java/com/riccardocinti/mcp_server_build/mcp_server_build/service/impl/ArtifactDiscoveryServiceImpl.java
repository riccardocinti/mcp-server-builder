package com.riccardocinti.mcp_server_build.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.mcp_server_build.exceptions.ArtifactDiscoveryException;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.ArtifactInfo;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.CompilationResult;
import com.riccardocinti.mcp_server_build.mcp_server_build.service.interfaces.ArtifactDiscoveryService;
import org.springframework.stereotype.Service;

@Service
public class ArtifactDiscoveryServiceImpl implements ArtifactDiscoveryService {
    @Override
    public ArtifactInfo discoverArtifacts(BuilderConfiguration builderConfiguration, CompilationResult compilationResult) throws ArtifactDiscoveryException {
        return null;
    }
}
