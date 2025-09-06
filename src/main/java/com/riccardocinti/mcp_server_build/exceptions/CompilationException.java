package com.riccardocinti.mcp_server_build.exceptions;

public class CompilationException extends McpServerBuilderException {
    public CompilationException(String message) {
        super(message);
    }

    public CompilationException(String message, Throwable e) {
        super(message, e);
    }
}
