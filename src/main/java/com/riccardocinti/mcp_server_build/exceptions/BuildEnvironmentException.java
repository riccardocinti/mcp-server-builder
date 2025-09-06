package com.riccardocinti.mcp_server_build.exceptions;

public class BuildEnvironmentException extends McpServerBuilderException {

    public BuildEnvironmentException(String message) {
        super(message);
    }

    public BuildEnvironmentException(String message, Throwable e) {
        super(message, e);
    }

}
