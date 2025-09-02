package com.riccardocinti.mcp_server_build.mcp_server_build.service.interfaces;

import com.riccardocinti.mcp_server_build.mcp_server_build.exceptions.CompilationException;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.CompilationResult;

public interface CompilationService {

    CompilationResult compileAndPackage(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) throws CompilationException;

}
