package com.riccardocinti.mcp_server_build.exceptions;

public class DependencyResolutionException extends McpServerBuilderException {

    public DependencyResolutionException(String message) {
        super(message);
    }

    public DependencyResolutionException(String message, Throwable e) {
        super(message, e);
    }

}
