package com.riccardocinti.mcp_server_build.model;

import java.util.List;

public record CompilationResult(boolean success,
                                List<String> compiledFiles,
                                List<String> errors,
                                List<String> warnings,
                                String output,
                                long duration) {
}
