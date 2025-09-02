package com.riccardocinti.mcp_server_build.mcp_server_build.service.impl;

import com.riccardocinti.mcp_server_build.mcp_server_build.exceptions.CompilationException;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuildEnvironment;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.BuilderConfiguration;
import com.riccardocinti.mcp_server_build.mcp_server_build.model.CompilationResult;
import com.riccardocinti.mcp_server_build.mcp_server_build.service.interfaces.CompilationService;
import org.springframework.stereotype.Service;

@Service
public class CompilationServiceImpl implements CompilationService {
    @Override
    public CompilationResult compileAndPackage(BuilderConfiguration builderConfiguration, BuildEnvironment buildEnv) throws CompilationException {
        return null;
    }
}
