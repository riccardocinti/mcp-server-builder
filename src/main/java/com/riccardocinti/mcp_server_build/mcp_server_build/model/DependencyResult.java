package com.riccardocinti.mcp_server_build.mcp_server_build.model;

import java.util.List;

public record DependencyResult(boolean success,
                               List<String> resolvedDependencies,
                               List<String> failedDependencies,
                               String output,
                               long duration) {
}
