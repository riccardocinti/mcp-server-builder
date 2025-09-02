package com.riccardocinti.mcp_server_build.mcp_server_build.service.interfaces;

import com.riccardocinti.mcp_server_build.mcp_server_build.exceptions.ArtifactDiscoveryException;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.ArtifactInfo;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.CompilationResult;

public interface ArtifactDiscoveryService {

    ArtifactInfo discoverArtifacts(BuilderConfiguration builderConfiguration, CompilationResult compilationResult) throws ArtifactDiscoveryException;

}
