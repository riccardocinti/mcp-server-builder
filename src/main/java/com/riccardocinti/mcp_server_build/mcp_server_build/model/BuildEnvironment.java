package com.riccardocinti.mcp_server_build.mcp_server_build.model;

import java.util.Map;

public record BuildEnvironment(Map<String, String> environmentVariables,
                               String workingDirectory,
                               String javaHome,
                               String buildToolHome,
                               String tempDirectory) {
}
